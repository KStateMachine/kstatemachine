package ru.nsk.kstatemachine.statemachine

import ru.nsk.kstatemachine.coroutines.CoroutineAbstraction
import ru.nsk.kstatemachine.event.*
import ru.nsk.kstatemachine.isSubStateOf
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.state.pseudo.UndoState
import ru.nsk.kstatemachine.transition.*
import ru.nsk.kstatemachine.transition.TransitionDirectionProducerPolicy.DefaultPolicy
import ru.nsk.kstatemachine.visitors.CheckUniqueNamesVisitor
import ru.nsk.kstatemachine.visitors.CleanupVisitor
import ru.nsk.kstatemachine.visitors.checkNonBlankNames
import kotlin.reflect.KClass

internal class StateMachineImpl(
    name: String?,
    childMode: ChildMode,
    override val creationArguments: StateMachine.CreationArguments,
    override val coroutineAbstraction: CoroutineAbstraction,
) : InternalStateMachine(name, childMode) {
    private val _machineListeners = mutableSetOf<StateMachine.Listener>()
    override val machineListeners: Collection<StateMachine.Listener> get() = _machineListeners
    override var logger: StateMachine.Logger = StateMachine.Logger {}
        set(value) {
            checkPropertyNotMutedOnRunningMachine(StateMachine.Logger::class)
            field = value
        }
    override var ignoredEventHandler = StateMachine.IgnoredEventHandler {}
        set(value) {
            checkPropertyNotMutedOnRunningMachine(StateMachine.IgnoredEventHandler::class)
            field = value
        }
    override var pendingEventHandler: StateMachine.PendingEventHandler = queuePendingEventHandler()
        set(value) {
            checkPropertyNotMutedOnRunningMachine(StateMachine.PendingEventHandler::class)
            field = value
        }
    override var listenerExceptionHandler = StateMachine.ListenerExceptionHandler { throw it }
        set(value) {
            checkPropertyNotMutedOnRunningMachine(StateMachine.ListenerExceptionHandler::class)
            field = value
        }
    private var _isDestroyed: Boolean = false
    override val isDestroyed get() = _isDestroyed

    /**
     * Flag for event processing mechanism, which takes place in [processEventBlocking] and during [startBlocking]/[startFrom].
     * It is not possible to process new event while previous processing is incomplete.
     */
    private var isProcessingEvent = false

    private var _isRunning = false
    override val isRunning get() = _isRunning

    private var delayedListenerException: Exception? = null

    private var _areListenersMuted = false
    override val areListenersMuted get() = _areListenersMuted

    init {
        transitionConditionally<StartEvent>("start transition") {
            direction = {
                when (event) {
                    is StartEventImpl -> {
                        if (event.startStates.size == 1) {
                            targetState(event.startState)
                        } else {
                            targetParallelStates(event.startStates)
                        }
                    }
                    is StartDataEventImpl<*> -> targetState(event.startState)
                }
            }
        }
        if (creationArguments.isUndoEnabled) {
            val undoState = addState(UndoState())
            transition<WrappedEvent>("undo transition", undoState)
        }
    }

    override fun openListenersMutationSection() = object : ListenersMutationSection {
        init {
            check(!_areListenersMuted) {
                "Seems ${ListenersMutationSection::class.simpleName} is already open, multiple simultaneous sections are not supported"
            }
            _areListenersMuted = true
        }

        override fun close() {
            _areListenersMuted = false
        }
    }

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

    override suspend fun start(argument: Any?): Unit = startFrom(setOf(this), argument)

    override suspend fun startFrom(states: Set<IState>, argument: Any?): Unit =
        doStartFrom(StartEventImpl(states), argument)

    override suspend fun <D : Any> startFrom(state: DataState<D>, data: D, argument: Any?): Unit =
        doStartFrom(StartDataEventImpl(state, data), argument)

    private suspend fun doStartFrom(event: StartEvent, argument: Any?): Unit =
        coroutineAbstraction.withContext {
            checkBeforeRunMachine()
            // fixme loosing this params (but similary (not same target) will be recreated on transition)
            val eventAndArgument = EventAndArgument(event, argument)
            eventProcessingScope {
                runCheckingExceptions {
                    val transitionParams = makeStartTransitionParams(event, this, event.startState, argument)
                    runMachine(transitionParams)
                    doProcessEvent(eventAndArgument)
                }
            }
        }

    private fun checkBeforeRunMachine() {
        accept(CheckUniqueNamesVisitor())
        if (creationArguments.requireNonBlankNames)
            checkNonBlankNames()
        checkNotDestroyed()
        check(!isRunning) { "$this is already started" }
        check(!isProcessingEvent) { "$this is already processing event, this is internal error, please report a bug" }
        if (childMode == ChildMode.EXCLUSIVE)
            requireInitialState()
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
            check(isRunning || event is DestroyEvent) { "$this is not started, call start() first" }

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
        return if (creationArguments.isUndoEnabled && event is UndoEvent) {
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
                    if (event.stop && isRunning) doStop()
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

        @Suppress("UNCHECKED_CAST")
        val targetStates = (direction.targetStates as Set<InternalState>).also { targetStates ->
            val alienState = targetStates.find { it !== this && !it.isSubStateOf(this) }
            check(alienState == null) {
                "Transitioning to targetState $alienState from another state machine is not possible"
            }
        }

        log {
            val targetsText = if (targetStates.isNotEmpty()) "to [${targetStates.joinToString()}]" else "[target-less]"
            "${event::class.simpleName} triggers $transition from ${transition.sourceState} $targetsText"
        }

        transition.transitionNotify { onTriggered(transitionParams) }
        machineNotify { onTransitionTriggered(transitionParams) }

        switchToTargetStates(targetStates, transition.sourceState, transitionParams)

        recursiveAfterTransitionComplete(transitionParams)

        val activeStates = activeStates()
        transition.transitionNotify { onComplete(transitionParams, activeStates) }
        machineNotify { onTransitionComplete(transitionParams, activeStates) }

        return true
    }

    /**
     * Starts machine if it is inner state of another one machine
     */
    override suspend fun doEnter(transitionParams: TransitionParams<*>) {
        if (!isRunning) startBlocking() else super.doEnter(transitionParams)
    }

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
        null,
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

private fun StateMachine.checkPropertyNotMutedOnRunningMachine(propertyType: KClass<*>) =
    check(!isRunning) { "Can not change ${propertyType.simpleName} after state machine started" }