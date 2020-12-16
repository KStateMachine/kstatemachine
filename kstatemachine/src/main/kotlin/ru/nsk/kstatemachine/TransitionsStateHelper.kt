package ru.nsk.kstatemachine

/**
 * Helper interface for [State] to keep transitions logic separately.
 */
interface TransitionsStateHelper {
    val transitions: Set<Transition<*>>

    fun <E : Event> addTransition(transition: Transition<E>): Transition<E>

    /**
     * Get transition by name. This might be used to start listening to transition after state machine setup.
     */
    fun findTransition(name: String) = transitions.find { it.name == name }
    fun requireTransition(name: String) =
        findTransition(name) ?: throw IllegalArgumentException("Transition $name not found")

    /**
     * For internal use only
     */
    fun asState(): State
}

/**
 * Get transition by Event class. This might be used to start listening to transition after state machine setup.
 */
inline fun <reified E : Event> TransitionsStateHelper.findTransition(): Transition<E>? {
    @Suppress("UNCHECKED_CAST")
    return transitions.find { it.eventMatcher.eventClass == E::class } as Transition<E>?
}

inline fun <reified E : Event> TransitionsStateHelper.requireTransition() =
    findTransition<E>() ?: throw IllegalArgumentException("Transition for ${E::class} not found")

/**
 * Creates simple transition.
 */
inline fun <reified E : Event> TransitionsStateHelper.transition(
    name: String? = null,
    block: (SimpleTransitionBuilder<E>.() -> Unit),
): Transition<E> {
    val builder = SimpleTransitionBuilder<E>().apply {
        eventMatcher = isInstanceOf()
        block()
    }

    val transition = DefaultTransition(builder.eventMatcher, asState(), builder.targetState, name)
    builder.listener?.let { transition.addListener(it) }
    return addTransition(transition)
}

/**
 * Overload for transition without any parameters.
 */
inline fun <reified E : Event> TransitionsStateHelper.transition(
    name: String? = null,
): Transition<E> =
    addTransition(DefaultTransition(EventMatcher.isInstanceOf(), asState(), name))

/**
 * Creates conditional transition. Caller should specify lambda which calculates [TransitionDirection].
 * For example target state may be different depending on some condition.
 */
inline fun <reified E : Event> TransitionsStateHelper.transitionConditionally(
    name: String? = null,
    block: ConditionalTransitionBuilder<E>.() -> Unit,
): Transition<E> {
    val builder = ConditionalTransitionBuilder<E>().apply {
        eventMatcher = isInstanceOf()
        block()
    }

    val transition = DefaultTransition(builder.eventMatcher, asState(), builder.direction, name)
    builder.listener?.let { transition.addListener(it) }
    return addTransition(transition)
}

/**
 * Creates guarded transition. Such transition is triggered only when guard function returns true.
 * Same behaviour might be achieved with conditional transition but guarded transition has simpler syntax.
 */
inline fun <reified E : Event> TransitionsStateHelper.transitionGuarded(
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

    val transition = DefaultTransition(builder.eventMatcher, asState(), direction, name)
    builder.listener?.let { transition.addListener(it) }
    return addTransition(transition)
}