package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.visitors.Visitor

@DslMarker
annotation class StateMachineDslMarker

interface StateMachine : State {
    var logger: Logger
    var ignoredEventHandler: IgnoredEventHandler
    var pendingEventHandler: PendingEventHandler
    val isRunning: Boolean
    val machineListeners: Collection<Listener>

    fun <L : Listener> addListener(listener: L): L
    fun removeListener(listener: Listener)

    /**
     * Starts state machine
     */
    fun start()

    /**
     * Forces state machine to stop
     */
    fun stop()

    /**
     * Machine must be started to process events
     */
    fun processEvent(event: Event, argument: Any? = null)

    /**
     * Set of states that the state machine is currently in, including machine itself.
     * Internal states of nested machines are not included.
     */
    fun activeStates(): Set<IState>

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
    init: StateMachineBlock
): StateMachine = StateMachineImpl(name, childMode).apply {
    init()
    if (start) start()
}

/**
 * Defines state machine API for internal library usage.
 */
interface InternalStateMachine : StateMachine, InternalState {
    fun startFrom(state: IState)
}

fun InternalStateMachine.machineNotify(block: StateMachine.Listener.() -> Unit) =
    machineListeners.forEach { it.apply(block) }

object Testing {
    /**
     * Method for testing purpose. It allows to start machine from particular [state]
     */
    fun StateMachine.startFrom(state: IState) = (this as InternalStateMachine).startFrom(state)
}