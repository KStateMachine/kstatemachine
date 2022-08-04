package ru.nsk.kstatemachine

sealed class TransitionDirection {
    /**
     * Already resolved target state of conditional transition or [PseudoState] computation
     */
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
 * [Transition] is triggered with a [targetState].
 */
internal class TargetState(override val targetState: IState) : TransitionDirection()

/**
 * [Transition] is triggered with a targetState, resolving it in place if it is a [PseudoState]
 */
fun EventAndArgument<*>.targetState(targetState: IState): TransitionDirection =
    TargetState(recursiveResolveTargetState(targetState))

private fun EventAndArgument<*>.recursiveResolveTargetState(targetState: IState): IState {
    return if (targetState is RedirectPseudoState)
        recursiveResolveTargetState(targetState.resolveTargetState(this))
    else
        targetState
}

/**
 * Internal use only. TODO remove it when possible
 */
internal fun unresolvedTargetState(targetState: IState): TransitionDirection = TargetState(targetState)

/**
 * Transition that matches event and has a meaningful direction (except [NoTransition])
 */
typealias ResolvedTransition<E> = Pair<InternalTransition<E>, TransitionDirection>

internal typealias TransitionDirectionProducer<E> = (TransitionDirectionProducerPolicy<E>) -> TransitionDirection

sealed class TransitionDirectionProducerPolicy<E : Event> {
    internal class DefaultPolicy<E : Event>(val eventAndArgument: EventAndArgument<E>) :
        TransitionDirectionProducerPolicy<E>() {
        override fun targetState(targetState: IState) = eventAndArgument.targetState(targetState)
        override fun targetStateOrStay(targetState: IState?) = targetState?.let { targetState(it) } ?: stay()
    }

    /**
     * TODO find the way to collect target states of conditional transitions
     */
    internal class CollectTargetStatesPolicy<E : Event> : TransitionDirectionProducerPolicy<E>() {
        override fun targetState(targetState: IState) = unresolvedTargetState(targetState)
        override fun targetStateOrStay(targetState: IState?) = targetState?.let { targetState(it) } ?: stay()
    }

    abstract fun targetState(targetState: IState): TransitionDirection
    abstract fun targetStateOrStay(targetState: IState?): TransitionDirection
}