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
    private val listeners: Set<Listener> = _listeners

    constructor(eventClass: Class<E>, sourceState: State, targetState: State?, name: String?) :
            this(eventClass, sourceState, name) {
        targetStateDirectionProducer = if (targetState == null) {
            { stay() }
        } else {
            { targetState(targetState) }
        }
    }

    constructor(
        eventClass: Class<E>,
        sourceState: State,
        targetStateDirectionProducer: () -> TransitionDirection,
        name: String?
    ) : this(eventClass, sourceState, name) {
        this.targetStateDirectionProducer = targetStateDirectionProducer
    }

    /**
     * Function that is called during event processing,
     * not during state machine configuration. So it is possible to check some outer (business logic) values in it.
     * If [Transition] does not have target state then [StateMachine] keeps current state
     * when such [Transition] is triggered.
     */
    private var targetStateDirectionProducer: () -> TransitionDirection = { stay() }

    fun produceTargetState(): State? {
        val direction = targetStateDirectionProducer()
        return if (direction is TARGETSTATE)
            direction.targetState
        else
            null
    }

    fun addListener(listener: Listener) {
        _listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        _listeners.remove(listener)
    }

    /**
     * Check if event can trigger this [Transition]
     * TODO add possibility to check concrete class or base class of hierarchy (current behaviour)
     */
    open fun isTriggeringEvent(event: Event): Boolean {
        return eventClass.isInstance(event)
    }

    fun notify(block: Listener.() -> Unit) = listeners.forEach { it.apply(block) }

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
