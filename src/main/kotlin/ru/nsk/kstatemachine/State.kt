package ru.nsk.kstatemachine

import java.util.concurrent.CopyOnWriteArraySet

open class State(val name: String) {
    private val _listeners = CopyOnWriteArraySet<Listener>()
    private val listeners: Set<Listener> = _listeners
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

    fun notify(block: Listener.() -> Unit) = listeners.forEach { it.apply(block) }

    override fun toString() = "${javaClass.simpleName}(name=$name)"

    interface Listener {
        fun onEntry(transitionParams: TransitionParams<*>) {}
        fun onExit(transitionParams: TransitionParams<*>) {}
    }
}

operator fun State.invoke(block: State.() -> Unit) = block()

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
    block: (TransitionBuilder<E>.() -> Unit)
): Transition<E> {
    val builder = TransitionBuilder<E>().apply(block)

    val transition = Transition(E::class.java, this, builder.targetState, name)
    builder.listener?.let { transition.addListener(it) }
    return addTransition(transition)
}

/**
 * Overload for transition without any parameters
 */
inline fun <reified E : Event> State.transition(name: String? = null): Transition<E> =
    addTransition(Transition(E::class.java, this, name))

class TransitionBuilder<E : Event> : BaseTransitionBuilder<E>() {
    var targetState: State? = null
}

/**
 * This method may be used if transition should be performed only if some condition is met,
 * or target state may vary depending on a condition.
 */
inline fun <reified E : Event> State.transitionConditionally(
    name: String? = null,
    block: ConditionalTransitionBuilder<E>.() -> Unit
): Transition<E> {
    val builder = ConditionalTransitionBuilder<E>().apply(block)

    val transition = Transition(E::class.java, this, builder.direction, name)
    builder.listener?.let { transition.addListener(it) }
    return addTransition(transition)
}

class ConditionalTransitionBuilder<E : Event> : BaseTransitionBuilder<E>() {
    lateinit var direction: () -> TransitionDirection
}

open class BaseTransitionBuilder<E : Event> {
    var listener: Transition.Listener? = null
}

inline fun <reified E : Event> BaseTransitionBuilder<E>.onTriggered(crossinline block: (TransitionParams<E>) -> Unit) {
    require(listener == null) { "Listener is already set, only one listener is allowed in a builder" }

    listener = object : Transition.Listener {
        override fun onTriggered(transitionParams: TransitionParams<*>) =
            block(transitionParams as TransitionParams<E>)
    }
}

sealed class TransitionDirection

/**
 * Transition is triggered, but state is not changed
 */
object STAY : TransitionDirection()

fun stay() = STAY

/**
 * Transition should not be triggered
 */
object NOTRANSITION : TransitionDirection()

fun noTransition() = NOTRANSITION

class TARGETSTATE(val targetState: State) : TransitionDirection()

fun targetState(targetState: State) = TARGETSTATE(targetState)
