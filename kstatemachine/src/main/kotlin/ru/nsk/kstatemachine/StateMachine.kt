package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.visitors.Visitor

@DslMarker
annotation class StateMachineDslMarker

interface StateMachine : State {
    var logger: Logger
    var ignoredEventHandler: IgnoredEventHandler
    var pendingEventHandler: PendingEventHandler

    /**
     * If machine catches exception from client code (listeners callbacks) it stores it until event processing
     * completes, and passes it to this handler. That keeps machine in well-defined predictable state and allows
     * to complete all required notifications.
     * Note that generally speaking listeners should not throw.
     *
     * Default implementation rethrows exception (only first one).
     * With your own handler you can mute or just log them for example.
     */
    var listenerExceptionHandler: ListenerExceptionHandler
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

    fun <L : Listener> addListener(listener: L): L
    fun removeListener(listener: Listener)

    /**
     * Starts state machine
     */
    fun start(argument: Any? = null)

    /**
     * Forces state machine to stop
     */
    fun stop()

    /**
     * Machine must be started to process events
     */
    fun processEvent(event: Event, argument: Any? = null)

    /**
     * Destroys machine structure clearing all listeners, states etc.
     */
    fun destroy(stop: Boolean = true)

    fun log(lazyMessage: () -> String)

    override fun accept(visitor: Visitor) = visitor.visit(this)

    interface Listener {
        /**
         * Notifies that state machine started (entered initial state).
         */
        fun onStarted() = Unit

        /**
         * This method is called when transition is performed.
         * There might be many transitions from one state to another,
         * this method might be used to listen to all transitions in one place
         * instead of listening for each transition separately.
         */
        fun onTransition(transitionParams: TransitionParams<*>) = Unit

        /**
         * Notifies about state changes.
         */
        fun onStateChanged(newState: IState) = Unit

        /**
         * Notifies that state machine has stopped.
         */
        fun onStopped() = Unit
    }

    /**
     * State machine uses this interface to support internal logging on different platforms
     */
    fun interface Logger {
        fun log(message: String)
    }

    fun interface IgnoredEventHandler {
        fun onIgnoredEvent(event: Event, argument: Any?)
    }

    fun interface PendingEventHandler {
        fun onPendingEvent(pendingEvent: Event, argument: Any?)
    }

    fun interface ListenerExceptionHandler {
        fun onException(exception: Exception)
    }
}

typealias StateMachineBlock = StateMachine.() -> Unit

fun StateMachine.onStarted(block: StateMachine.() -> Unit) {
    addListener(object : StateMachine.Listener {
        override fun onStarted() = block()
    })
}

fun StateMachine.onStopped(block: StateMachine.() -> Unit) {
    addListener(object : StateMachine.Listener {
        override fun onStopped() = block()
    })
}

fun StateMachine.onTransition(block: StateMachine.(TransitionParams<*>) -> Unit) {
    addListener(object : StateMachine.Listener {
        override fun onTransition(transitionParams: TransitionParams<*>) =
            block(transitionParams)
    })
}

fun StateMachine.onStateChanged(block: StateMachine.(newState: IState) -> Unit) {
    addListener(object : StateMachine.Listener {
        override fun onStateChanged(newState: IState) = block(newState)
    })
}

/**
 * Factory method for creating [StateMachine]
 */
fun createStateMachine(
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    start: Boolean = true,
    autoDestroyOnStatesReuse: Boolean = true,
    init: StateMachineBlock
): StateMachine = StateMachineImpl(name, childMode, autoDestroyOnStatesReuse).apply {
    init()
    if (start) start()
}