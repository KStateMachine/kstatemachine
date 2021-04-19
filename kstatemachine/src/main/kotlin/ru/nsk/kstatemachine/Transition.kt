package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.visitors.Visitor
import ru.nsk.kstatemachine.visitors.VisitorAcceptor

/**
 * Base interface for events which may trigger transitions of [StateMachine]
 */
interface Event

/**
 * Event holding some data
 */
interface DataEvent<out D> : Event {
    val data: D
}

/**
 * Event without data
 * FIXME currently this interface is useless and can be safely removed
 */
interface UnitEvent : Event

/**
 * Represent a transition between states, which gets triggered when specified [Event] is posted to [StateMachine]
 */
interface Transition<E : Event> : VisitorAcceptor {
    val name: String?
    val eventMatcher: EventMatcher<E>
    val sourceState: State

    /**
     * This parameter may be used to pass arbitrary data with a transition to targetState.
     * This argument must be set from transition listener. Such transition must have only one listener
     * that sets the argument.
     */
    var argument: Any?
    val listeners: Collection<Listener>

    fun <L : Listener> addListener(listener: L): L
    fun removeListener(listener: Listener)

    /**
     * Checks if the [event] matches this [Transition]
     */
    fun isMatchingEvent(event: Event): Boolean

    override fun accept(visitor: Visitor) = visitor.visit(this)

    interface Listener {
        fun onTriggered(transitionParams: TransitionParams<*>) = Unit
    }
}

inline fun <reified E : Event> Transition<E>.onTriggered(crossinline block: (TransitionParams<E>) -> Unit) {
    addListener(object : Transition.Listener {
        @Suppress("UNCHECKED_CAST")
        override fun onTriggered(transitionParams: TransitionParams<*>) = block(transitionParams as TransitionParams<E>)
    })
}

@StateMachineDslMarker
data class TransitionParams<E : Event>(
    val transition: Transition<E>,
    val direction: TransitionDirection,
    val event: E,
    /**
     * This parameter may be used to pass arbitrary data with the event,
     * so there is no need to define [Event] subclasses every time.
     * Subclassing should be preferred if the event always contains data of some type.
     */
    val argument: Any? = null,
)

/**
 * Defines transition API for internal library usage. All transitions must implement this interface.
 */
interface InternalTransition<E : Event> : Transition<E> {
    override val sourceState: InternalState
    fun produceTargetStateDirection(): TransitionDirection
}

internal fun InternalTransition<*>.transitionNotify(block: Transition.Listener.() -> Unit) =
    listeners.forEach { it.apply(block) }
