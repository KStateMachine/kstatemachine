package ru.nsk.kstatemachine

sealed class TransitionDirection {
    open val targetState: IState? = null
}

/**
 * [Transition] is triggered, but state is not changed
 */
internal object Stay : TransitionDirection()

fun stay(): TransitionDirection = Stay

/**
 * [Transition] should not be triggered
 */
internal object NoTransition : TransitionDirection()

fun noTransition(): TransitionDirection = NoTransition

/**
 * [Transition] is triggered with a [targetState]
 */
internal class TargetState(override val targetState: IState) : TransitionDirection()

fun targetState(targetState: IState): TransitionDirection = TargetState(targetState)

/**
 * Transition that matches event and has a meaningful direction (except [NoTransition])
 */
typealias ResolvedTransition<E> = Pair<InternalTransition<E>, TransitionDirection>

internal typealias TransitionDirectionProducer<E> = (TransitionDirectionProducerPolicy<E>) -> TransitionDirection

sealed class TransitionDirectionProducerPolicy<E : Event> {
    class DefaultPolicy<E : Event>(val event: E) : TransitionDirectionProducerPolicy<E>()

    /**
     * TODO find the way to collect target states of conditional transitions
     */
    class CollectTargetStatesPolicy<E : Event> : TransitionDirectionProducerPolicy<E>()
}