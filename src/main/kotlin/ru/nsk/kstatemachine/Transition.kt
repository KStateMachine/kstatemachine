package ru.nsk.kstatemachine

import java.util.concurrent.CopyOnWriteArraySet

/**
 * Base interface for events which may trigger transitions of [StateMachine]
 */
interface Event

/**
 * Represent a transition between states, which gets triggered when specified [Event] is posted to [StateMachine]
 */
open class Transition<E : Event>(private val eventClass: Class<E>, val sourceState: State, val name: String?) {
    private val _listeners = CopyOnWriteArraySet<Listener>()
    val listeners: Set<Listener> = _listeners

    /**
     * If [Transition] does not have [targetState] then [StateMachine] keeps current [State]
     * when such [Transition] is triggered
     */
    var targetState: State? = null
        set(state) {
            require(sourceState !== targetState)
            field = state
        }

    /**
     * Condition predicate.
     * [Transition] may be triggered only if predicate returns true
     */
    var condition: (Event) -> Boolean = { true }

    fun addListener(listener: Listener) {
        _listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        _listeners.remove(listener)
    }

    /**
     * Check if event can trigger this [Transition]
     */
    open fun isTriggeringEvent(event: Event): Boolean {
        return eventClass.isInstance(event) && condition(event)
    }

    override fun toString() = "${javaClass.simpleName}(name=$name)"

    interface Listener {
        fun onTriggered(transitionParams: TransitionParams<*>) {}
    }
}

inline fun <reified E : Event> Transition<E>.onTriggered(crossinline block: (TransitionParams<E>) -> Unit) {
    addListener(object : Transition.Listener {
        override fun onTriggered(transitionParams: TransitionParams<*>) = block(transitionParams as TransitionParams<E>)
    })
}
