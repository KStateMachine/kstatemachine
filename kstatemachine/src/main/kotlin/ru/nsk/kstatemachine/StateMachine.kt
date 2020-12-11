package ru.nsk.kstatemachine

@DslMarker
annotation class StateMachineDslMarker

@StateMachineDslMarker
interface StateMachine : InternalState {
    var logger: Logger
    var ignoredEventHandler: IgnoredEventHandler
    var pendingEventHandler: PendingEventHandler

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

    fun processEvent(event: Event, argument: Any? = null)

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }

    interface Listener {
        /**
         * Notifies that state machine started (entered initial state).
         */
        fun onStarted() = Unit

        /**
         * This method is called when transition is performed.
         * There might be may transitions from one state to another,
         * this method might be used to listen to all transitions in one place
         * instead of listening for each transition separately.
         */
        fun onTransition(sourceState: State, targetState: State?, event: Event, argument: Any?) = Unit

        /**
         * Notifies about state changes.
         * This method will also be triggered on adding listener with a current state of a state machine.
         */
        fun onStateChanged(newState: State) = Unit

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
        fun onIgnoredEvent(currentState: State, event: Event, argument: Any?)
    }

    fun interface PendingEventHandler {
        fun onPendingEvent(pendingEvent: Event, argument: Any?)
    }
}


typealias StateBlock = State.() -> Unit
typealias StateMachineBlock = StateMachine.() -> Unit

fun StateMachine.onStarted(block: StateMachine.() -> Unit) {
    addListener(object : StateMachine.Listener {
        override fun onStarted() = block()
    })
}

fun StateMachine.onTransition(
    block: StateMachine.(
        sourceState: State,
        targetState: State?,
        event: Event,
        argument: Any?
    ) -> Unit
) {
    addListener(object : StateMachine.Listener {
        override fun onTransition(sourceState: State, targetState: State?, event: Event, argument: Any?) =
            block(sourceState, targetState, event, argument)
    })
}

fun StateMachine.onStateChanged(block: StateMachine.(newState: State) -> Unit) {
    addListener(object : StateMachine.Listener {
        override fun onStateChanged(newState: State) = block(newState)
    })
}

/**
 * Factory method for creating [StateMachine]
 */
fun createStateMachine(
    name: String? = null,
    start: Boolean = true,
    init: StateMachineBlock
): StateMachine = StateMachineImpl(name).apply {
    init()
    if (start) start()
}

@StateMachineDslMarker
data class TransitionParams<E : Event>(
    val transition: Transition<E>,
    val event: E,
    /**
     * This parameter may be used to pass arbitrary data with the event,
     * so there is no need to define [Event] subclasses every time.
     * Subclassing should be preferred if the event always contains data of some type.
     */
    val argument: Any? = null,
)
