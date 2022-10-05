package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.visitors.CheckUniqueNamesVisitor
import ru.nsk.kstatemachine.visitors.CleanupVisitor

/**
 * Defines state machine API for internal library usage.
 */
abstract class InternalStateMachine(name: String?, childMode: ChildMode) : StateMachine, DefaultState(name, childMode) {
    internal abstract fun startFrom(state: IState, argument: Any?)
    internal abstract fun delayListenerException(exception: Exception)
}

internal class StateMachineImpl(
    name: String?,
    childMode: ChildMode,
    override val autoDestroyOnStatesReuse: Boolean,
    override val isUndoEnabled: Boolean,
) : InternalStateMachine(name, childMode) {
    private val _machineListeners = mutableSetOf<StateMachine.Listener>()
    override val machineListeners: Collection<StateMachine.Listener> get() = _machineListeners
    override var logger: StateMachine.Logger = NullLogger
    override var ignoredEventHandler = StateMachine.IgnoredEventHandler { _, _ -> }
    override var pendingEventHandler = queuePendingEventHandler()
    override var listenerExceptionHandler = StateMachine.ListenerExceptionHandler { throw it }
    private var _isDestroyed: Boolean = false
    override val isDestroyed get() = _isDestroyed

    init {
        if (isUndoEnabled) {
            val undoState = addState(UndoState())
            transition<WrappedEvent>("undo transition", undoState)
        }
    }

    /**
     * Flag for event processing mechanism, which takes place in [processEvent] and during [start]/[startFrom].
     * It is not possible to process new event while previous processing is incomplete.
     */
    private var isProcessingEvent = false

    private var _isRunning = false
    override val isRunning get() = _isRunning

    private var delayedListenerException: Exception? = null

    override fun delayListenerException(exception: Exception) {
        if (delayedListenerException == null)
            delayedListenerException = exception
    }

    private object NullLogger : StateMachine.Logger {
        override fun log(message: String) {}
    }

    override fun <L : StateMachine.Listener> addListener(listener: L): L {
        require(_machineListeners.add(listener)) { "$listener is already added" }
        return listener
    }

    override fun removeListener(listener: StateMachine.Listener) {
        _machineListeners.remove(listener)
    }

    override fun start(argument: Any?) = startFrom(this, argument)

    override fun startFrom(state: IState, argument: Any?) {
        checkBeforeRunMachine()

        eventProcessingScope {
            runCheckingExceptions {
                val transitionParams = makeStartTransitionParams(this, state, argument)
                runMachine(transitionParams)
                switchToTargetState(state as InternalState, this, transitionParams)
                recursiveAfterTransitionComplete(transitionParams)
            }
        }
    }

    private fun checkBeforeRunMachine() {
        accept(CheckUniqueNamesVisitor())
        check(!isDestroyed) { "$this is already destroyed" }
        check(!isRunning) { "$this is already started" }
        check(!isProcessingEvent) { "$this is already processing event, this is internal error, please report a bug" }
        if (childMode == ChildMode.EXCLUSIVE)
            checkNotNull(initialState) { "Initial state is not set, call setInitialState() first" }
    }

    private fun runMachine(transitionParams: TransitionParams<*>) {
        _isRunning = true
        log { "$this started" }
        machineNotify { onStarted() }
        doEnter(transitionParams)
    }

    override fun stop() {
        check(!isDestroyed) { "$this is already destroyed" }
        if (!_isRunning) return

        runCheckingExceptions {
            _isRunning = false
            recursiveStop()
            log { "$this stopped" }
            machineNotify { onStopped() }
        }
    }

    override fun processEvent(event: Event, argument: Any?) {
        check(!isDestroyed) { "$this is already destroyed" }
        check(isRunning) { "$this is not started, call start() first" }

        val eventAndArgument = wrapEvent(event, argument)

        if (isProcessingEvent) {
            pendingEventHandler.onPendingEvent(eventAndArgument.event, eventAndArgument.argument)
            // pending event cannot be processed while previous event is still processing
            // even if PendingEventHandler does not throw. QueuePendingEventHandler implementation stores such events
            // to be processed later.
            return
        }

        eventProcessingScope {
            process(eventAndArgument)
        }
    }

    private fun wrapEvent(event: Event, argument: Any?): EventAndArgument<*> {
        return if (isUndoEnabled && event is UndoEvent) {
            val wrapped = requireState<UndoState>().makeWrappedEvent()
            EventAndArgument(wrapped, argument)
        } else {
            EventAndArgument(event, argument)
        }
    }

    private fun process(eventAndArgument: EventAndArgument<*>) {
        var eventProcessed: Boolean? = null

        runCheckingExceptions {
            eventProcessed = doProcessEvent(eventAndArgument)
        }

        if (eventProcessed == false) {
            log { "$this ignored ${eventAndArgument.event::class.simpleName}" }
            ignoredEventHandler.onIgnoredEvent(eventAndArgument.event, eventAndArgument.argument)
        }
    }

    /**
     * Runs block of code that processes event, and processes all pending events from queue after it if
     * [QueuePendingEventHandler] is used.
     */
    private fun eventProcessingScope(block: () -> Unit) {
        val queue = pendingEventHandler as? QueuePendingEventHandler
        queue?.checkEmpty()

        isProcessingEvent = true
        try {
            block()

            queue?.let {
                var eventAndArgument = it.nextEventAndArgument()
                while (eventAndArgument != null) {
                    process(eventAndArgument)

                    eventAndArgument = it.nextEventAndArgument()
                }
            }
        } catch (e: Exception) {
            queue?.clear()
            throw e
        } finally {
            isProcessingEvent = false
        }
    }

    /**
     * Runs block of code that triggers notification listeners
     */
    private fun runCheckingExceptions(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            log { "Fatal exception happened, $this machine is in unpredictable state and will be destroyed: $e" }
            runCatching { destroy(false) }
            throw e
        }
        delayedListenerException?.let {
            delayedListenerException = null
            listenerExceptionHandler.onException(it)
        }
    }

    private fun <E : Event> doProcessEvent(eventAndArgument: EventAndArgument<E>): Boolean {
        val (event, argument) = eventAndArgument
        if (isFinished) {
            log { "$this is finished, skipping event ${event::class.simpleName}, with argument $argument" }
            return false
        }

        val (transition, direction) = recursiveFindUniqueResolvedTransition(eventAndArgument) ?: return false

        val transitionParams = TransitionParams(transition, direction, event, argument)

        val targetState = (direction.targetState as? InternalState)?.also {
            check(it === this || it.isSubStateOf(this)) {
                "Transitioning to state $it from another state machine is not possible"
            }
        }

        log {
            val targetText = if (targetState != null) "to $targetState" else "[target-less]"
            "${event::class.simpleName} triggers $transition from ${transition.sourceState} $targetText"
        }

        transition.transitionNotify { onTriggered(transitionParams) }
        machineNotify { onTransition(transitionParams) }

        targetState?.let { switchToTargetState(it, transition.sourceState, transitionParams) }

        recursiveAfterTransitionComplete(transitionParams)

        val activeStates = activeStates()
        machineNotify { onTransitionComplete(transitionParams, activeStates) }

        return true
    }

    override fun log(lazyMessage: () -> String) {
        if (logger != NullLogger)
            logger.log(lazyMessage())
    }

    /**
     * Starts machine if it is inner state of another one machine
     */
    override fun doEnter(transitionParams: TransitionParams<*>) =
        if (!isRunning) start() else super.doEnter(transitionParams)


    override fun cleanup() {
        _machineListeners.clear()
        super.cleanup()
    }

    override fun destroy(stop: Boolean) {
        if (stop) stop()
        accept(CleanupVisitor())
        _isDestroyed = true
    }
}

internal fun InternalStateMachine.machineNotify(block: StateMachine.Listener.() -> Unit) {
    machineListeners.toList().forEach { runDelayingException { it.block() } }
}

internal fun InternalStateMachine.runDelayingException(block: () -> Unit) =
    try {
        block()
    } catch (e: Exception) {
        delayListenerException(e)
    }