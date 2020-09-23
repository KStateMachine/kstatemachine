package ru.nsk.kstatemachine

/**
 * Simple finite state machine (FSM) implementation.
 */
class StateMachine(val name: String?, private val logger: Logger?) {
    private val states = mutableSetOf<State>()
    private lateinit var initialState: State
    private lateinit var currentState: State

    fun addState(state: State): State {
        states += state
        return state
    }

    /**
     * Now initial state is mandatory, but if we add parallel states it will not be mandatory
     */
    fun setInitialState(state: State) {
        require(states.contains(state)) { "$state is not part of $this machine, use addState() first" }
        initialState = state
        currentState = initialState
    }

    fun log(message: String) = logger?.log(message)

    fun postEvent(event: Event, argument: Any? = null) {
        val fromState = currentState
        val transition = fromState.findTransition(event)

        if (transition != null) {
            log("$this triggering $transition from $fromState")
            transition.listeners.forEach { it.onTriggered(TransitionParams(transition, event, argument)) }

            transition.targetState?.let { targetState ->
                log("$this exiting $fromState")
                fromState.listeners.forEach { it.onExit(TransitionParams(transition, event, argument)) }

                currentState = targetState
                log("$this entering $targetState")
                targetState.listeners.forEach { it.onEntry(TransitionParams(transition, event, argument)) }
            }
        } else {
            log("$this skipping $event as transition from $fromState, was not found")
        }
    }

    override fun toString() = "${javaClass.simpleName}(name=$name)"

    private fun <E : Event> State.findTransition(event: E): Transition<E>? {
        val triggeringTransitions = transitions.filter { it.isTriggeringEvent(event) }
        check(triggeringTransitions.size <= 1) { "Multiple transitions match $event $triggeringTransitions in $this" }
        return triggeringTransitions.firstOrNull() as Transition<E>?
    }

    fun interface Logger {
        fun log(message: String)
    }
}

fun StateMachine.state(name: String, init: (State.() -> Unit)? = null): State {
    val state = State(name)
    if (init != null) state.init()
    return addState(state)
}

fun createStateMachine(
    name: String? = null,
    logger: StateMachine.Logger? = null,
    init: StateMachine.() -> Unit
) = StateMachine(name, logger).apply(init)

data class TransitionParams<E : Event>(
    val transition: Transition<E>,
    val event: E,
    /**
     * This parameter may be used to pass arbitrary data with the event,
     * so there is no need to define [Event] subclasses every time.
     * Subclassing should be preferred if the event always contains data of some type.
     */
    val argument: Any? = null
)