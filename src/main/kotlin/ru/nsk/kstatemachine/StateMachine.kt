package ru.nsk.kstatemachine

@DslMarker
annotation class StateMachineDslMarker

@StateMachineDslMarker
interface StateMachine {
    val name: String?
    val states: Set<State>
    var logger: Logger
    var ignoredEventHandler: IgnoredEventHandler
    var pendingEventHandler: PendingEventHandler

    fun <L : Listener> addListener(listener: L): L
    fun removeListener(listener: Listener)

    fun <S : State> addState(state: S, init: StateBlock? = null): S

    /**
     * A shortcut for [addState] and [setInitialState] calls
     */
    fun <S : State> addInitialState(state: S, init: StateBlock? = null): S

    /**
     * Currently initial state is mandatory, but if we add parallel states it might change.
     */
    fun setInitialState(state: State)

    /**
     * Get state by name. This might be used to start listening to state after state machine setup.
     */
    fun findState(name: String): State?
    fun requireState(name: String): State

    fun processEvent(event: Event, argument: Any? = null)

    interface Listener {
        /**
         * This method is called when transition is performed.
         * There might be may transitions from one state to another,
         * this method might be used to listen to all transitions in one place
         * instead of listening for each transition separately.
         */
        fun onTransition(sourceState: State, targetState: State?, event: Event, argument: Any?) {}

        /**
         * Notifies about state changes.
         * This method will also be triggered on adding listener with a current state of a state machine.
         */
        fun onStateChanged(newState: State) {}
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

fun StateMachine.onTransition(block: (sourceState: State, targetState: State?, event: Event, argument: Any?) -> Unit) {
    addListener(object : StateMachine.Listener {
        override fun onTransition(sourceState: State, targetState: State?, event: Event, argument: Any?) =
            block(sourceState, targetState, event, argument)
    })
}

fun StateMachine.onStateChanged(block: (newState: State) -> Unit) {
    addListener(object : StateMachine.Listener {
        override fun onStateChanged(newState: State) = block(newState)
    })
}

/**
 * @param name is optional and is useful for getting state instance after state machine setup
 * with [StateMachine.findState] and for debugging.
 */
fun StateMachine.state(name: String? = null, init: StateBlock? = null) = addState(State(name), init)

/**
 * A shortcut for [state] and [StateMachine.setInitialState] calls
 */
fun StateMachine.initialState(name: String? = null, init: StateBlock? = null): State {
    val state = addState(State(name), init)
    setInitialState(state)
    return state
}

/**
 * Factory method for creating [StateMachine]
 */
fun createStateMachine(
    name: String? = null,
    init: StateMachineBlock
): StateMachine = StateMachineImpl(name).apply {
    init()
    start()
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