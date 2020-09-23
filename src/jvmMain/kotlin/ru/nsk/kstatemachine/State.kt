package ru.nsk.kstatemachine

import java.util.concurrent.CopyOnWriteArraySet

open class State(val name: String) {
    private val _listeners = CopyOnWriteArraySet<Listener>()
    val listeners: Set<Listener> = _listeners
    private val _transitions = mutableSetOf<Transition<*>>()
    val transitions: Set<Transition<*>> = _transitions

    fun <E : Event> addTransition(transition: Transition<E>): Transition<E> {
        _transitions += transition
        return transition
    }

    fun addListener(listener: Listener) {
        _listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        _listeners.remove(listener)
    }

    override fun toString() = "${javaClass.simpleName}(name=$name)"

    interface Listener {
        fun onEntry(transitionParams: TransitionParams<*>) {}
        fun onExit(transitionParams: TransitionParams<*>) {}
    }
}

fun State.onEntry(block: (TransitionParams<*>) -> Unit) {
    addListener(object : State.Listener {
        override fun onEntry(transitionParams: TransitionParams<*>) = block(transitionParams)
    })
}

fun State.onExit(block: (TransitionParams<*>) -> Unit) {
    addListener(object : State.Listener {
        override fun onExit(transitionParams: TransitionParams<*>) = block(transitionParams)
    })
}

inline fun <reified E : Event> State.transition(
    name: String? = null,
    noinline block: (Transition<E>.() -> Unit)? = null
): Transition<E> {
    val transition = Transition(E::class.java, this, name)
    if (block != null) transition.block()
    return addTransition(transition)
}