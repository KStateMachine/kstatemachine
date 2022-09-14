package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.EventMatcher.Companion.isInstanceOf

/**
 * Helper interface for [IState] to keep transitions logic separately.
 */
interface TransitionStateApi {
    val transitions: Set<Transition<*>>

    fun <E : Event> addTransition(transition: Transition<E>): Transition<E>

    /**
     * For internal use only
     */
    fun asState(): IState
}

/**
 * Find transition by name. This might be used to start listening to transition after state machine setup.
 */
fun TransitionStateApi.findTransition(name: String) = transitions.find { it.name == name }

fun TransitionStateApi.requireTransition(name: String) =
    requireNotNull(findTransition(name)) { "Transition $name not found" }

/**
 * Find transition by Event type. This might be used to start listening to transition after state machine setup.
 */
inline fun <reified E : Event> TransitionStateApi.findTransition(): Transition<E>? {
    @Suppress("UNCHECKED_CAST")
    return transitions.find { it.eventMatcher.eventClass == E::class } as Transition<E>?
}

/**
 * Require transition by Event type
 */
inline fun <reified E : Event> TransitionStateApi.requireTransition() =
    requireNotNull(findTransition<E>()) { "Transition for ${E::class.simpleName} not found" }

/**
 * Shortcut overload for transition with an optional target state
 */
inline fun <reified E : Event> TransitionStateApi.transition(
    name: String? = null,
    targetState: State? = null
): Transition<E> = addTransition(DefaultTransition(name, isInstanceOf(), asState(), targetState))

/**
 * Creates transition.
 * You can specify guard function. Such guarded transition is triggered only when guard function returns true.
 *
 * This is a special kind of conditional transition but with simpler syntax and less flexibility.
 */
inline fun <reified E : Event> TransitionStateApi.transition(
    name: String? = null,
    block: UnitGuardedTransitionBuilder<E>.() -> Unit,
): Transition<E> {
    val builder = UnitGuardedTransitionBuilder<E>(name, asState()).apply {
        eventMatcher = isInstanceOf()
        block()
    }
    return addTransition(builder.build())
}

/**
 * This is more powerful version of [transition] function.
 * Here target state is a lambda which returns desired State.
 * This allows to use lateinit state variables for recursively depending states and
 * choose target state depending on application business logic.
 *
 * This is a special kind of conditional transition but with simpler syntax and less flexibility.
 */
inline fun <reified E : Event> TransitionStateApi.transitionOn(
    name: String? = null,
    block: UnitGuardedTransitionOnBuilder<E>.() -> Unit,
): Transition<E> {
    val builder = UnitGuardedTransitionOnBuilder<E>(name, asState()).apply {
        eventMatcher = isInstanceOf()
        block()
    }
    return addTransition(builder.build())
}

/**
 * Creates conditional transition. Caller should specify lambda which calculates [TransitionDirection].
 * For example target state may be different depending on some condition.
 */
inline fun <reified E : Event> TransitionStateApi.transitionConditionally(
    name: String? = null,
    block: ConditionalTransitionBuilder<E>.() -> Unit,
): Transition<E> {
    val builder = ConditionalTransitionBuilder<E>(name, asState()).apply {
        eventMatcher = isInstanceOf()
        block()
    }
    return addTransition(builder.build())
}

/**
 * Shortcut function for type safe argument transition.
 * Data transition can not be target-less as it does not make sense.
 */
inline fun <reified E : DataEvent<D>, D : Any> TransitionStateApi.dataTransition(
    name: String? = null,
    targetState: DataState<D>
): Transition<E> {
    require(targetState != asState()) {
        "data transition should no be self targeted, use simple transition instead"
    }
    return addTransition(DefaultTransition(name, isInstanceOf(), asState(), targetState))
}

/**
 * Creates type safe argument transition to [DataState].
 */
inline fun <reified E : DataEvent<D>, D : Any> TransitionStateApi.dataTransition(
    name: String? = null,
    block: DataGuardedTransitionBuilder<E, D>.() -> Unit,
): Transition<E> {
    val builder = DataGuardedTransitionBuilder<E, D>(name, asState()).apply {
        eventMatcher = isInstanceOf()
        block()
    }
    requireNotNull(builder.targetState) {
        "data transition should no be target-less, specify targetState or use simple transition instead"
    }
    require(builder.targetState != asState()) {
        "data transition should no be self targeted, use simple transition instead"
    }
    return addTransition(builder.build())
}

/**
 * Data transition, otherwise same as [transitionOn]
 */
inline fun <reified E : DataEvent<D>, D : Any> TransitionStateApi.dataTransitionOn(
    name: String? = null,
    block: DataGuardedTransitionOnBuilder<E, D>.() -> Unit,
): Transition<E> {
    val builder = DataGuardedTransitionOnBuilder<E, D>(name, asState()).apply {
        eventMatcher = isInstanceOf()
        block()
    }
    return addTransition(builder.build())
}
