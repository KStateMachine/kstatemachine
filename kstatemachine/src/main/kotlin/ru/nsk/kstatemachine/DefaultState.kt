package ru.nsk.kstatemachine

import java.util.concurrent.CopyOnWriteArraySet

open class DefaultState(override val name: String? = null) : InternalState {
    private val _listeners = CopyOnWriteArraySet<State.Listener>()

    private val _states = mutableSetOf<State>()
    override val states: Set<State> = _states

    private var _initialState: InternalState? = null
    override val initialState
        get() = _initialState

    private val _transitions = mutableSetOf<Transition<*>>()
    override val transitions: Set<Transition<*>> = _transitions

    override fun <E : Event> addTransition(transition: Transition<E>): Transition<E> {
        _transitions += transition
        return transition
    }

    override fun <L : State.Listener> addListener(listener: L): L {
        require(_listeners.add(listener)) { "$listener is already added" }
        return listener
    }

    override fun removeListener(listener: State.Listener) {
        _listeners.remove(listener)
    }

    override fun <S : State> addState(state: S, init: StateBlock?): S {
        check(!isStarted) { "Can not add state after state machine started" }

        require(!_states.contains(state)) { "$state already added" }
        val name = state.name
        if (name != null)
            require(findState(name) == null) { "State with name $name already exists" }

        if (init != null) state.init()
        _states += state
        return state
    }

    override fun findState(name: String) = states.find { it.name == name }
    override fun requireState(name: String) = findState(name) ?: throw IllegalArgumentException("State $name not found")

    override fun setInitialState(state: State) {
        require(states.contains(state)) { "$state is not part of $this machine, use addState() first" }
        check(!isStarted) { "Can not change initial state after state machine started" }

        _initialState = state as InternalState
    }

    /**
     * Get transition by name. This might be used to start listening to transition after state machine setup.
     */
    override fun findTransition(name: String) = transitions.find { it.name == name }
    override fun requireTransition(name: String) =
        findTransition(name) ?: throw IllegalArgumentException("Transition $name not found")

    override fun notify(block: State.Listener.() -> Unit) = _listeners.forEach { it.apply(block) }

    override fun <E : Event> findTransitionByEvent(event: E): InternalTransition<E>? {
        val triggeringTransitions = transitions.filter { it.isTriggeringEvent(event) }
        check(triggeringTransitions.size <= 1) { "Multiple transitions match $event $triggeringTransitions in $this" }
        @Suppress("UNCHECKED_CAST")
        return triggeringTransitions.firstOrNull() as InternalTransition<E>?
    }

    override fun toString() = "${this::class.simpleName}(name=$name)"
}

class DefaultFinalState(name: String? = null) : DefaultState(name), FinalState {
    override fun <E : Event> addTransition(transition: Transition<E>) =
        throw UnsupportedOperationException("FinalState can not have transitions")
}