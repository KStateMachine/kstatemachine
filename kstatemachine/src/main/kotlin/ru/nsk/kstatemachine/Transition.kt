package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.visitors.Visitor
import ru.nsk.kstatemachine.visitors.VisitorAcceptor

/**
 * Base interface for events which may trigger transitions of [StateMachine]
 */
interface Event

/**
 * Represent a transition between states, which gets triggered when specified [Event] is posted to [StateMachine]
 */
interface Transition<E : Event> : VisitorAcceptor {
    val eventMatcher: EventMatcher<E>
    val sourceState: State
    val name: String?

    /**
     * This parameter may be used to pass arbitrary data with a transition to targetState.
     * This argument must be set from transition listener. Such transition must have only one listener setting argument.
     */
    var argument: Any?

    fun <L : Listener> addListener(listener: L): L
    fun removeListener(listener: Listener)

    /**
     * Check if event can trigger this [Transition]
     */
    fun isTriggeringEvent(event: Event): Boolean

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }

    interface Listener {
        fun onTriggered(transitionParams: TransitionParams<*>) = Unit
    }
}

/**
 * Defines transition API for internal library usage. All transitions must implement this interface.
 */
interface InternalTransition<E : Event> : Transition<E> {
    fun produceTargetStateDirection(): TransitionDirection
    fun notify(block: Transition.Listener.() -> Unit)
}

inline fun <reified E : Event> Transition<E>.onTriggered(crossinline block: (TransitionParams<E>) -> Unit) {
    addListener(object : Transition.Listener {
        @Suppress("UNCHECKED_CAST")
        override fun onTriggered(transitionParams: TransitionParams<*>) = block(transitionParams as TransitionParams<E>)
    })
}
