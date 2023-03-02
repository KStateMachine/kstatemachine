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
internal open class TargetState(override val targetState: IState) : TransitionDirection()

/**
 * [Transition] is triggered with a targetState, resolving it in place if it is a [PseudoState]
 */
suspend fun EventAndArgument<*>.targetState(targetState: IState) = recursiveResolveTargetState(targetState)

private suspend fun EventAndArgument<*>.recursiveResolveTargetState(targetState: IState): TransitionDirection {
    return when (targetState) {
        is RedirectPseudoState -> recursiveResolveTargetState(targetState.resolveTargetState(this))
        is HistoryState -> TargetState(targetState.storedState)
        is UndoState -> targetState.popState()?.let { TargetState(it) } ?: NoTransition
        else -> TargetState(targetState)
    }
}

/**
 * Internal use only. TODO remove it when possible
 */
internal fun unresolvedTargetState(targetState: IState): TransitionDirection = TargetState(targetState)

/**
 * Transition that matches event and has a meaningful direction (except [NoTransition])
 */
typealias ResolvedTransition<E> = Pair<InternalTransition<E>, TransitionDirection>

internal typealias TransitionDirectionProducer<E> = suspend (TransitionDirectionProducerPolicy<E>) -> TransitionDirection

sealed class TransitionDirectionProducerPolicy<E : Event> {
    internal class DefaultPolicy<E : Event>(val eventAndArgument: EventAndArgument<E>) :
        TransitionDirectionProducerPolicy<E>() {
        override suspend fun targetState(targetState: IState) = eventAndArgument.targetState(targetState)
        override suspend fun targetStateOrStay(targetState: IState?) = targetState?.let { targetState(it) } ?: stay()
    }

    /**
     * TODO find the way to collect target states of conditional transitions
     */
    internal class CollectTargetStatesPolicy<E : Event> : TransitionDirectionProducerPolicy<E>() {
        override suspend fun targetState(targetState: IState) = unresolvedTargetState(targetState)
        override suspend fun targetStateOrStay(targetState: IState?) = targetState?.let { targetState(it) } ?: stay()
    }

    abstract suspend fun targetState(targetState: IState): TransitionDirection
    abstract suspend fun targetStateOrStay(targetState: IState?): TransitionDirection
}