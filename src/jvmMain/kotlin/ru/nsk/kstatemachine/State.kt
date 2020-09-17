package ru.nsk.kstatemachine

import java.util.concurrent.CopyOnWriteArraySet

open class State(val name: String) {
    private val _listeners = CopyOnWriteArraySet<Listener>()
    val listeners: Set<Listener> = _listeners
    private val _transitions = mutableSetOf<Transition>()
    val transitions: Set<Transition> = _transitions

    fun addTransition(transition: Transition): Transition {
        _transitions += transition
        return transition
    }

    fun addListener(listener: Listener) = _listeners.add(listener)
    fun removeListener(listener: Listener) = _listeners.remove(listener)

    interface Listener {
        fun onEntry(transition: Transition) {}
        fun onExit(transition: Transition) {}
    }
}

fun State.onEntry(block: (transition: Transition) -> Unit) {
    addListener(object : State.Listener {
        override fun onEntry(transition: Transition) = block(transition)
    })
}

fun State.onExit(block: (transition: Transition) -> Unit) {
    addListener(object : State.Listener {
        override fun onExit(transition: Transition) = block(transition)
    })
}

fun <E: Event>State.transition(block: (Transition.() -> Unit)? = null): Transition {
    val transition = EventTransition<E>(this)
    if (block != null) transition.block()
    return addTransition(transition)
}