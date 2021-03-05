package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.EventMatcher.Companion.isInstanceOf

/**
 * Helper interface for [State] to keep transitions logic separately.
 */
interface StateTransitionsHelper {
    val transitions: Set<Transition<*>>

    fun <E : Event> addTransition(transition: Transition<E>): Transition<E>

    /**
     * Get transition by name. This might be used to start listening to transition after state machine setup.
     */
    fun findTransition(name: String) = transitions.find { it.name == name }

    /**
     * For internal use only
     */
    fun asState(): State
}

fun StateTransitionsHelper.requireTransition(name: String) =
    requireNotNull(findTransition(name)) { "Transition $name not found" }

/**
 * Get transition by Event class. This might be used to start listening to transition after state machine setup.
 */
inline fun <reified E : Event> StateTransitionsHelper.findTransition(): Transition<E>? {
    @Suppress("UNCHECKED_CAST")
    return transitions.find { it.eventMatcher.eventClass == E::class } as Transition<E>?
}

inline fun <reified E : Event> StateTransitionsHelper.requireTransition() =
    requireNotNull(findTransition<E>()) { "Transition for ${E::class} not found" }

/**
 * Overload for transition without any parameters.
 */
inline fun <reified E : Event> StateTransitionsHelper.transition(
    name: String? = null,
): Transition<E> =
    addTransition(DefaultTransition(name, isInstanceOf(), asState()))

/**
 * Creates transition.
 * You can specify guard function. Such guarded transition is triggered only when guard function returns true.
 *
 * This is a special kind of conditional transition but with simpler syntax and less flexibility.
 */
inline fun <reified E : Event> StateTransitionsHelper.transition(
    name: String? = null,
    block: GuardedTransitionBuilder<E>.() -> Unit,
): Transition<E> {
    val builder = GuardedTransitionBuilder<E>().apply {
        eventMatcher = isInstanceOf()
        block()
    }

    val direction = {
        if (builder.guard()) {
            val target = builder.targetState
            if (target == null) stay() else targetState(target)
        } else {
            noTransition()
        }
    }

    val transition = DefaultTransition(name, builder.eventMatcher, asState(), direction)
    builder.listener?.let { transition.addListener(it) }
    return addTransition(transition)
}

inline fun <A : Any, reified E : ArgEvent<A>> StateTransitionsHelper.argTransition(
    name: String? = null,
    block: GuardedArgTransitionBuilder<A, E>.() -> Unit,
): Transition<E> {
    val builder = GuardedArgTransitionBuilder<A, E>().apply {
        eventMatcher = isInstanceOf()
        block()
    }

    val direction = {
        if (builder.guard()) {
            val target = builder.targetState
            if (target == null) stay() else targetState(target)
        } else {
            noTransition()
        }
    }

    val transition = DefaultTransition(name, builder.eventMatcher, asState(), direction)
    builder.listener?.let { transition.addListener(it) }
    return addTransition(transition)
}

/**
 * This is more powerful version of [transition] function.
 * Here target state is a lambda which returns desired State.
 * This allows to use lateinit state variables for recursively depending states and
 * choose target state depending on application business logic.
 *
 * This is a special kind of conditional transition but with simpler syntax and less flexibility.
 */
inline fun <reified E : Event> StateTransitionsHelper.transitionTo(
    name: String? = null,
    block: GuardedTransitionToBuilder<E>.() -> Unit,
): Transition<E> {
    val builder = GuardedTransitionToBuilder<E>().apply {
        eventMatcher = isInstanceOf()
        block()
    }

    val direction = {
        if (builder.guard()) targetState(builder.targetState()) else noTransition()
    }

    val transition = DefaultTransition(name, builder.eventMatcher, asState(), direction)
    builder.listener?.let { transition.addListener(it) }
    return addTransition(transition)
}

/**
 * Creates conditional transition. Caller should specify lambda which calculates [TransitionDirection].
 * For example target state may be different depending on some condition.
 */
inline fun <reified E : Event> StateTransitionsHelper.transitionConditionally(
    name: String? = null,
    block: ConditionalTransitionBuilder<E>.() -> Unit,
): Transition<E> {
    val builder = ConditionalTransitionBuilder<E>().apply {
        eventMatcher = isInstanceOf()
        block()
    }

    val transition = DefaultTransition(name, builder.eventMatcher, asState(), builder.direction)
    builder.listener?.let { transition.addListener(it) }
    return addTransition(transition)
}