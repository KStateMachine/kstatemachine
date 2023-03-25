package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.TransitionDirectionProducerPolicy.DefaultPolicy
import ru.nsk.kstatemachine.visitors.CheckUniqueNamesVisitor
import ru.nsk.kstatemachine.visitors.CleanupVisitor

/**
 * Defines state machine API for internal library usage.
 */
abstract class InternalStateMachine(name: String?, childMode: ChildMode) :
    BuildingStateMachine, DefaultState(name, childMode) {
    internal abstract suspend fun startFrom(state: IState, argument: Any?)
    internal abstract suspend fun <D : Any> startFrom(state: DataState<D>, data: D, argument: Any?)
    internal abstract fun delayListenerException(exception: Exception)
}

internal class StateMachineImpl(
    name: String?,
    childMode: ChildMode,
    override val autoDestroyOnStatesReuse: Boolean,
    override val isUndoEnabled: Boolean,
    override val doNotThrowOnMultipleTransitionsMatch: Boolean,
    override val coroutineAbstraction: CoroutineAbstraction,
) : InternalStateMachine(name, childMode) {
    private val _machineListeners = mutableSetOf<StateMachine.Listener>()
    override val machineListeners: Collection<StateMachine.Listener> get() = _machineListeners
    override var logger: StateMachine.Logger = StateMachine.Logger {}
    override var ignoredEventHandler = StateMachine.IgnoredEventHandler {}
    override var pendingEventHandler: StateMachine.PendingEventHandler = queuePendingEventHandler()
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
     * Flag for event processing mechanism, which takes place in [processEventBlocking] and during [startBlocking]/[startFrom].
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

    override fun <L : StateMachine.Listener> addListener(listener: L): L {
        require(_machineListeners.add(listener)) { "$listener is already added" }
        return listener
    }

    override fun removeListener(listener: StateMachine.Listener) {
        _machineListeners.remove(listener)
    }

    override suspend fun start(argument: Any?) = startFrom(this, argument)

    override suspend fun startFrom(state: IState, argument: Any?) =
        doStartFrom(StartEventImpl(), state, argument)

    override suspend fun <D : Any> startFrom(state: DataState<D>, data: D, argument: Any?) =
        doStartFrom(StartDataEventImpl(data), state, argument)

    private suspend fun doStartFrom(event: StartEvent, state: IState, argument: Any?) =
        coroutineAbstraction.withContext {
            checkBeforeRunMachine()

            eventProcessingScope {
                runCheckingExceptions {
                    val transitionParams = makeStartTransitionParams(event, this, state, argument)
                    runMachine(transitionParams)
                    switchToTargetState(state as InternalState, this, transitionParams)
                    recursiveAfterTransitionComplete(transitionParams)
                }
            }
        }

    private fun checkBeforeRunMachine() {
        accept(CheckUniqueNamesVisitor())
        checkNotDestroyed()
        check(!isRunning) { "$this is already started" }
        check(!isProcessingEvent) { "$this is already processing event, this is internal error, please report a bug" }
        if (childMode == ChildMode.EXCLUSIVE)
            checkNotNull(initialState) { "Initial state is not set, call setInitialState() first" }
    }

    /** To be called only from [runCheckingExceptions] */
    private suspend fun runMachine(transitionParams: TransitionParams<*>) {
        _isRunning = true
        log { "$this started" }
        machineNotify { onStarted() }
        doEnter(transitionParams)
    }

    /** To be called only from [runCheckingExceptions] */
    private suspend fun doStop() {
        _isRunning = false
        recursiveStop()
        log { "$this stopped" }
        machineNotify { onStopped() }
    }

    override suspend fun processEvent(event: Event, argument: Any?): ProcessingResult {
        return coroutineAbstraction.withContext {
            checkNotDestroyed()
            check(isRunning) { "$this is not started, call start() first" }

            val eventAndArgument = EventAndArgument(event, argument)

            if (isProcessingEvent) {
                pendingEventHandler.onPendingEvent(eventAndArgument)
                // pending event cannot be processed while previous event is still processing
                // even if PendingEventHandler does not throw. QueuePendingEventHandler implementation stores such events
                // to be processed later.
                return@withContext ProcessingResult.PENDING
            }

            eventProcessingScope {
                process(eventAndArgument)
            }
        }
    }

    private fun EventAndArgument<*>.wrap(): EventAndArgument<*> {
        return if (isUndoEnabled && event is UndoEvent) {
            val wrapped = requireState<UndoState>().makeWrappedEvent()
            EventAndArgument(wrapped, argument)
        } else {
            this
        }
    }

    private suspend fun process(eventAndArgument: EventAndArgument<*>): ProcessingResult {
        val wrappedEventAndArgument = eventAndArgument.wrap()

        val eventProcessed = runCheckingExceptions {
            when (val event = wrappedEventAndArgument.event) {
                is StopEvent -> {
                    doStop()
                    true
                }

                is DestroyEvent -> {
                    if (event.stop) doStop()
                    doDestroy()
                    true
                }

                else -> doProcessEvent(wrappedEventAndArgument)
            }
        }

        if (!eventProcessed) {
            log { "$this ignored ${wrappedEventAndArgument.event::class.simpleName}" }
            ignoredEventHandler.onIgnoredEvent(wrappedEventAndArgument)
        }
        return if (eventProcessed) ProcessingResult.PROCESSED else ProcessingResult.IGNORED
    }

    /**
     * Runs block of code that processes event, and processes all pending events from queue after it if
     * [QueuePendingEventHandler] is used.
     */
    private suspend fun <R> eventProcessingScope(block: suspend () -> R): R {
        val queue = pendingEventHandler as? QueuePendingEventHandler
        queue?.checkEmpty()

        val result: R
        isProcessingEvent = true
        try {
            result = block()

            queue?.let {
                var eventAndArgument = it.nextEventAndArgument()
                while (eventAndArgument != null) {
                    if (isDestroyed || !isRunning) { // if it happens while event processing
                        it.clear()
                        return result
                    }
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
        return result
    }

    /**
     * Runs block of code that triggers notification listeners
     */
    private suspend fun <R> runCheckingExceptions(block: suspend () -> R): R {
        val result: R
        try {
            result = block()
        } catch (e: Exception) {
            log { "Fatal exception happened, $this machine is in unpredictable state and will be destroyed: $e" }
            runCatching { doDestroy() }
            throw e
        }
        delayedListenerException?.let {
            delayedListenerException = null
            listenerExceptionHandler.onException(it)
        }
        return result
    }

    private suspend fun <E : Event> doProcessEvent(eventAndArgument: EventAndArgument<E>): Boolean {
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
        machineNotify { onTransitionTriggered(transitionParams) }

        targetState?.let { switchToTargetState(it, transition.sourceState, transitionParams) }

        recursiveAfterTransitionComplete(transitionParams)

        val activeStates = activeStates()
        transition.transitionNotify { onComplete(transitionParams, activeStates) }
        machineNotify { onTransitionComplete(transitionParams, activeStates) }

        return true
    }

    /**
     * Starts machine if it is inner state of another one machine
     */
    override suspend fun doEnter(transitionParams: TransitionParams<*>) =
        if (!isRunning) startBlocking() else super.doEnter(transitionParams)


    override suspend fun cleanup() {
        _machineListeners.clear()
        super.cleanup()
    }

    /** To be called only from [runCheckingExceptions] */
    private suspend fun doDestroy() {
        _isDestroyed = true
        machineNotify { onDestroyed() }
        log { "$this destroyed" }
        accept(CleanupVisitor())
    }
}

internal fun StateMachine.checkNotDestroyed() = check(!isDestroyed) { "$this is already destroyed" }

internal suspend inline fun InternalStateMachine.runDelayingException(crossinline block: suspend () -> Unit) =
    try {
        block()
    } catch (e: Exception) {
        delayListenerException(e)
    }

internal suspend inline fun <reified E : StartEvent> makeStartTransitionParams(
    event: E,
    sourceState: IState,
    targetState: IState = sourceState,
    argument: Any?
): TransitionParams<*> {
    val transition = DefaultTransition(
        "Starting",
        EventMatcher.isInstanceOf<E>(),
        TransitionType.LOCAL,
        sourceState,
        targetState,
    )

    return TransitionParams(
        transition,
        transition.produceTargetStateDirection(DefaultPolicy(EventAndArgument(event, argument))),
        event,
        argument,
    )
}