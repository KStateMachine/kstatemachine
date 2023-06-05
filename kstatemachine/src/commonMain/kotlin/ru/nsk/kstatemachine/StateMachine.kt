package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.StateMachine.PendingEventHandler
import ru.nsk.kstatemachine.visitors.CoVisitor
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
    suspend fun start(argument: Any? = null)

    /**
     * Processes [Event].
     * Machine must be started to be able to process events.
     * @return [ProcessingResult] for current event.
     * If more events will be queued while this method is working, there results will not be taken to account.
     * Their [processEventBlocking] calls will return [ProcessingResult.PENDING] in this case.
     */
    suspend fun processEvent(event: Event, argument: Any? = null): ProcessingResult

    override suspend fun accept(visitor: CoVisitor) = coroutineAbstraction.withContext {
        visitor.visit(this)
    }

    override fun accept(visitor: Visitor) = visitor.visit(this)

    /**
     * Provides [StateMachine] specific notifications and also duplicates [IState.Listener] and [Transition.Listener]
     * functionality allowing to listen all notifications in one place if necessary.
     */
    interface Listener {
        /**
         * Notifies that state machine started (entered initial state).
         */
        suspend fun onStarted() = Unit

        /**
         * This method is called when any transition is triggered/performed.
         */
        suspend fun onTransitionTriggered(transitionParams: TransitionParams<*>) = Unit

        /**
         * Same as [onTransitionTriggered] but called after transition is complete and provides set of current active states.
         */
        suspend fun onTransitionComplete(transitionParams: TransitionParams<*>, activeStates: Set<IState>) = Unit

        /**
         * Notifies about any child state entry (including nested states).
         */
        suspend fun onStateEntry(state: IState, transitionParams: TransitionParams<*>) = Unit
        suspend fun onStateExit(state: IState, transitionParams: TransitionParams<*>) = Unit
        suspend fun onStateFinished(state: IState, transitionParams: TransitionParams<*>) = Unit

        /**
         * Notifies that state machine has stopped.
         */
        suspend fun onStopped() = Unit

        /**
         * Notifies that state machine has destroyed.
         */
        suspend fun onDestroyed() = Unit
    }

    /**
     * State machine uses this interface to support internal logging on different platforms
     */
    fun interface Logger {
        /** Message is lazy for performance reasons */
        suspend fun log(lazyMessage: () -> String)
    }

    fun interface IgnoredEventHandler {
        suspend fun onIgnoredEvent(eventAndArgument: EventAndArgument<*>)
    }

    fun interface PendingEventHandler {
        suspend fun onPendingEvent(eventAndArgument: EventAndArgument<*>)
    }

    fun interface ListenerExceptionHandler {
        suspend fun onException(exception: Exception)
    }
}

fun StateMachine.startBlocking(argument: Any? = null) = coroutineAbstraction.runBlocking { start(argument) }

/**
 * Blocking analog of [StateMachine.processEvent] which can be called from usual (not suspendable) code.
 */
fun StateMachine.processEventBlocking(event: Event, argument: Any? = null) = coroutineAbstraction.runBlocking {
    processEvent(event, argument)
}

/**
 * Shortcut for [StateMachine.stopBlocking] and [StateMachine.start] sequence calls
 */
suspend fun StateMachine.restart(argument: Any? = null) {
    stop()
    start(argument)
}

fun StateMachine.restartBlocking(argument: Any? = null) = coroutineAbstraction.runBlocking { restart(argument) }

/**
 * Rolls back transition (usually it is navigating machine to previous state).
 * Previous states are stored in a stack, so this method mey be called multiple times if needed.
 * This function has same effect as alternative syntax processEvent(UndoEvent), but throws if undo feature is not enabled.
 */
suspend fun StateMachine.undo(argument: Any? = null): ProcessingResult = coroutineAbstraction.withContext {
    check(isUndoEnabled) {
        "Undo functionality is not enabled, use createStateMachine(enableUndo = true) argument to enable it."
    }
    return@withContext processEvent(UndoEvent, argument)
}

/**
 * Blocking analog of [undo]
 */
fun StateMachine.undoBlocking(argument: Any? = null) = coroutineAbstraction.runBlocking { undo(argument) }

/**
 * Suspendable [stopBlocking] analog. Should be preferred especially if called from machine notifications.
 */
suspend fun StateMachine.stop() = coroutineAbstraction.withContext {
    checkNotDestroyed()
    if (!isRunning) return@withContext
    processEvent(StopEvent)
}

/**
 * Forces state machine to stop
 * Warning: calling this function from notification callback may cause deadlock
 * if you are using single threaded coroutineContext, so [stop] should be preferred.
 */
fun StateMachine.stopBlocking() = coroutineAbstraction.runBlocking { stop() }

/**
 * Destroys machine structure clearing all listeners, states etc.
 */
suspend fun StateMachine.destroy(stop: Boolean = true) = coroutineAbstraction.withContext {
    if (isDestroyed) return@withContext
    processEvent(DestroyEvent(stop))
}

/**
 * Blocking analog of [destroy]
 */
fun StateMachine.destroyBlocking(stop: Boolean = true) = coroutineAbstraction.runBlocking { destroy(stop) }

suspend fun IState.log(lazyMessage: () -> String) {
    machineOrNull()?.logger?.log(lazyMessage)
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
 * Factory method for creating [StateMachine].
 * Suspendable code will be called via Kotlin Standard library (without Kotlin Coroutines library support).
 */
fun createStdLibStateMachine(
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    start: Boolean = true,
    autoDestroyOnStatesReuse: Boolean = true,
    enableUndo: Boolean = false,
    doNotThrowOnMultipleTransitionsMatch: Boolean = false,
    init: suspend BuildingStateMachine.() -> Unit
): StateMachine {
    return with(StdLibCoroutineAbstraction()) {
        runBlocking {
            createStateMachine(
                name,
                childMode,
                start,
                autoDestroyOnStatesReuse,
                enableUndo,
                doNotThrowOnMultipleTransitionsMatch,
                init
            )
        }
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