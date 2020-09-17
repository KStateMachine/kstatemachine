package ru.nsk.kstatemachine

/**
 * Simple finite state machine (FSM) implementation.
 */
class StateMachine(private val name: String?) {
    private val states = mutableSetOf<State>()
    private lateinit var initialState: State
    private lateinit var currentState: State

    fun addState(state: State) : State {
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

    fun postEvent(event: Event) {
        val fromState = currentState
        val transition = fromState.findTransition(event)
        if (transition != null) {
            transition.listeners.forEach { it.onTriggered(transition) }

            transition.targetState?.let { targetState ->
                fromState.listeners.forEach { it.onExit(transition) }
                currentState = targetState
                targetState.listeners.forEach { it.onEntry(transition) }
            }
        } else {
            println("$name drops $event as transition from ${fromState.name}, was not found")
        }
    }

    private fun State.findTransition(event: Event): Transition? {
        val triggeringTransitions = transitions.filter { it.isTriggeringEvent(event) }
        check(triggeringTransitions.size <= 1) { "Multiple transitions match event $triggeringTransitions" }
        return triggeringTransitions.firstOrNull()
    }
}

fun StateMachine.state(name: String, init: (State.() -> Unit)? = null): State {
    val state = State(name)
    if (init != null) state.init()
    return addState(state)
}

fun createStateMachine(name: String? = null, init: StateMachine.() -> Unit) = StateMachine(name).apply(init)