package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.StateMachine.PendingEventHandler
import ru.nsk.kstatemachine.visitors.Visitor

@DslMarker
annotation class StateMachineDslMarker

interface StateMachine : State {
    val logger: Logger
    val ignoredEventHandler: IgnoredEventHandler
    val pendingEventHandler: PendingEventHandler

    /**
     * If machine catches exception from client code (listeners callbacks) it stores it until event processing
     * completes, and passes it to this handler. That keeps machine in well-defined predictable state and allows
     * to complete all required notifications.
     * Note that generally speaking listeners should not throw.
     *
     * Default implementation rethrows exception (only first one).
     * With your own handler you can mute or just log them for example.
     */
    val listenerExceptionHandler: ListenerExceptionHandler
    val isRunning: Boolean
    val machineListeners: Collection<Listener>

    /**
     * Allows the library to automatically call destroy() on current state owning machine instance if user tries
     * to reuse its states in another machine. Usually this is a result of using object states in sequentially created
     * similar machines. destroy() will be called on the previous machine instance.
     * If set to false an exception will be thrown on state reuse attempt.
     */
    val autoDestroyOnStatesReuse: Boolean
    val isDestroyed: Boolean

    val isUndoEnabled: Boolean

    /**
     * If set to true, when multiple transitions match event the first matching transition is selected.
     * if set to false, when multiple transitions match event exception is thrown.
     * Default if false.
     */
    val doNotThrowOnMultipleTransitionsMatch: Boolean

    val coroutineAbstraction: CoroutineAbstraction

    fun <L : Listener> addListener(listener: L): L
    fun removeListener(listener: Listener)

    /**
     * Starts state machine
     */
    fun start(argument: Any? = null)

    /**
     * Processes [Event].
     * Machine must be started to be able to process events.
     * @return [ProcessingResult] for current event.
     * If more events will be queued while this method is working, there results will not be taken to account.
     * Their [processEvent] calls will return [ProcessingResult.PENDING] in this case.
     */
    fun processEvent(event: Event, argument: Any? = null): ProcessingResult

    /**
     * Same as [processEvent] but suspendable, so user may easily call it in blocking and non-blocking way.
     * This method is based on [CoroutineAbstraction.withContext] internally.
     */
    suspend fun processEventCo(event: Event, argument: Any? = null): ProcessingResult

    fun log(lazyMessage: () -> String)

    override fun accept(visitor: Visitor) = visitor.visit(this)

    interface Listener {
        /**
         * Notifies that state machine started (entered initial state).
         */
        suspend fun onStarted() = Unit

        /**
         * This method is called when transition is performed.
         * There might be many transitions from one state to another,
         * this method might be used to listen to all transitions in one place
         * instead of listening for each transition separately.
         */
        suspend fun onTransition(transitionParams: TransitionParams<*>) = Unit

        /**
         * Same as [onTransition] but called after transition is complete and provides set of current active states.
         */
        suspend fun onTransitionComplete(transitionParams: TransitionParams<*>, activeStates: Set<IState>) = Unit

        /**
         * Notifies about child state entry (including nested states).
         */
        suspend fun onStateEntry(state: IState) = Unit

        /**
         * Notifies that state machine has stopped.
         */
        suspend fun onStopped() = Unit
    }

    /**
     * State machine uses this interface to support internal logging on different platforms
     */
    fun interface Logger {
        fun log(message: String)
    }

    fun interface IgnoredEventHandler {
        fun onIgnoredEvent(eventAndArgument: EventAndArgument<*>)
    }

    fun interface PendingEventHandler {
        fun onPendingEvent(eventAndArgument: EventAndArgument<*>)
    }

    fun interface ListenerExceptionHandler {
        fun onException(exception: Exception)
    }
}

/**
 * Shortcut for [StateMachine.stop] and [StateMachine.start] sequence calls
 */
fun StateMachine.restart(argument: Any? = null) {
    stop()
    start(argument)
}

/**
 * Rolls back transition (usually it is navigating machine to previous state).
 * Previous states are stored in a stack, so this method mey be called multiple times if needed.
 * This function has same effect as alternative syntax processEvent(UndoEvent), but throws if undo feature is not enabled.
 */
fun StateMachine.undo(argument: Any? = null) = coroutineAbstraction.runBlocking { undoCo(argument) }

suspend fun StateMachine.undoCo(argument: Any? = null): ProcessingResult = coroutineAbstraction.withContext {
    check(isUndoEnabled) {
        "Undo functionality is not enabled, use createStateMachine(isUndoEnabled = true) argument to enable it."
    }
    return@withContext processEventCo(UndoEvent, argument)
}

/**
 * Forces state machine to stop
 */
fun StateMachine.stop() = coroutineAbstraction.runBlocking { stopCo() }

suspend fun StateMachine.stopCo() = coroutineAbstraction.withContext {
    checkNotDestroyed()
    if (!isRunning) return@withContext
    processEventCo(StopEvent)
}


/**
 * Destroys machine structure clearing all listeners, states etc.
 */
fun StateMachine.destroy(stop: Boolean = true) = coroutineAbstraction.runBlocking { destroyCo(stop) }

suspend fun StateMachine.destroyCo(stop: Boolean = true) = coroutineAbstraction.withContext {
    if (isDestroyed) return@withContext
    processEventCo(DestroyEvent(stop))
}

/**
 * Allows to mutate some properties, which is necessary during setup, before machine is started
 */
interface BuildingStateMachine : StateMachine {
    override var logger: StateMachine.Logger
    override var ignoredEventHandler: StateMachine.IgnoredEventHandler
    override var pendingEventHandler: PendingEventHandler
    override var listenerExceptionHandler: StateMachine.ListenerExceptionHandler
}

/**
 * Factory method for creating [StateMachine]
 */
fun createStateMachine(
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    start: Boolean = true,
    autoDestroyOnStatesReuse: Boolean = true,
    enableUndo: Boolean = false,
    doNotThrowOnMultipleTransitionsMatch: Boolean = false,
    coroutineAbstraction: CoroutineAbstraction = StdLibCoroutineAbstraction(),
    init: BuildingStateMachine.() -> Unit
): StateMachine {
    return StateMachineImpl(
        name,
        childMode,
        autoDestroyOnStatesReuse,
        enableUndo,
        doNotThrowOnMultipleTransitionsMatch,
        coroutineAbstraction,
    ).apply {
        init()
        if (start) start()
    }
}

enum class ProcessingResult {
    /** Event was sent to [PendingEventHandler] */
    PENDING,

    /** Event was processed */
    PROCESSED,

    /** Event was ignored */
    IGNORED,
}