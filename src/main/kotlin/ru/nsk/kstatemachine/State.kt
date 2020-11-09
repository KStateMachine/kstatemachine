package ru.nsk.kstatemachine

import java.util.concurrent.CopyOnWriteArraySet

@StateMachineDslMarker
open class State(val name: String? = null) {
    private val _listeners = CopyOnWriteArraySet<Listener>()
    private val listeners: Set<Listener> = _listeners
    private val _transitions = mutableSetOf<Transition<*>>()
    private val transitions: Set<Transition<*>> = _transitions

    fun <E : Event> addTransition(transition: Transition<E>): Transition<E> {
        _transitions += transition
        return transition
    }

    fun <L : Listener> addListener(listener: L): L {
        require(_listeners.add(listener)) { "$listener is already added" }
        return listener
    }

    fun removeListener(listener: Listener) {
        _listeners.remove(listener)
    }

    internal fun notify(block: Listener.() -> Unit) = listeners.forEach { it.apply(block) }

    internal fun <E : Event> findTransition(event: E): Transition<E>? {
        val triggeringTransitions = transitions.filter { it.isTriggeringEvent(event) }
        check(triggeringTransitions.size <= 1) { "Multiple transitions match $event $triggeringTransitions in $this" }
        return triggeringTransitions.firstOrNull() as Transition<E>?
    }

    override fun toString() = "${javaClass.simpleName}(name=$name)"

    interface Listener {
        fun onEntry(transitionParams: TransitionParams<*>) {}
        fun onExit(transitionParams: TransitionParams<*>) {}
    }
}

operator fun <S : State> S.invoke(block: S.() -> Unit) = block()

fun <S : State> S.onEntry(block: S.(TransitionParams<*>) -> Unit) {
    addListener(object : State.Listener {
        override fun onEntry(transitionParams: TransitionParams<*>) = block(transitionParams)
    })
}

fun <S : State> S.onExit(block: S.(TransitionParams<*>) -> Unit) {
    addListener(object : State.Listener {
        override fun onExit(transitionParams: TransitionParams<*>) = block(transitionParams)
    })
}

inline fun <reified E : Event> State.transition(
    name: String? = null,
    block: (SimpleTransitionBuilder<E>.() -> Unit),
): Transition<E> {
    val builder = SimpleTransitionBuilder<E>().apply {
        eventMatcher = isInstanceOf()
        block()
    }

    val transition = Transition<E>(builder.eventMatcher, this, builder.targetState, name)
    builder.listener?.let { transition.addListener(it) }
    return addTransition(transition)
}

/**
 * Overload for transition without any parameters
 */
inline fun <reified E : Event> State.transition(
    name: String? = null,
): Transition<E> =
    addTransition(Transition(EventMatcher.isInstanceOf<E>(), this, name))

/**
 * This method may be used if transition should be performed only if some condition is met,
 * or target state may vary depending on a condition.
 */
inline fun <reified E : Event> State.transitionConditionally(
    name: String? = null,
    block: ConditionalTransitionBuilder<E>.() -> Unit,
): Transition<E> {
    val builder = ConditionalTransitionBuilder<E>().apply {
        eventMatcher = isInstanceOf()
        block()
    }

    val transition = Transition<E>(builder.eventMatcher, this, builder.direction, name)
    builder.listener?.let { transition.addListener(it) }
    return addTransition(transition)
}