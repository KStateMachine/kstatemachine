package ru.nsk.kstatemachine

import java.util.concurrent.CopyOnWriteArraySet


abstract class Transition(val sourceState: State) {
    private val _listeners = CopyOnWriteArraySet<Listener>()
    val listeners: Set<Listener> = _listeners
    /**
     * It's ok to have null [targetState] for [Transition]
     */
    var targetState: State? = null

    fun addListener(listener: Listener) = _listeners.add(listener)
    fun removeListener(listener: Listener) = _listeners.remove(listener)

    /**
     * Check if current [Event] can trigger this [Transition]
     */
    abstract fun isTriggeringEvent(event: Event): Boolean

    interface Listener {
        fun onTriggered(transition: Transition) {}
    }
}

fun Transition.onTriggered(block: (transition: Transition) -> Unit) {
    addListener(object: Transition.Listener {
        override fun onTriggered(transition: Transition) = block(transition)
    })
}

class EventTransition<E: Event>(sourceState: State): Transition(sourceState) {
    override fun isTriggeringEvent(event: Event): Boolean {
        return false
    }
}

interface Event
