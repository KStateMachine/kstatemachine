package ru.nsk.kstatemachine

import java.util.concurrent.CopyOnWriteArraySet

open class DefaultState(override val name: String? = null) : InternalState {
    private val _listeners = CopyOnWriteArraySet<State.Listener>()
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

    override fun toString() = "${javaClass.simpleName}(name=$name)"
}