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
suspend fun EventAndArgument<*>.targetState(targetState: IState): TransitionDirection = resolveTargetState(targetState)

private suspend fun EventAndArgument<*>.resolveTargetState(targetState: IState): TransitionDirection {
    val resolvedState = recursiveResolveTargetState(targetState)
    return if (resolvedState != null) TargetState(resolvedState) else NoTransition
}

private suspend fun EventAndArgument<*>.recursiveResolveTargetState(targetState: IState): IState? {
    val resolvedTarget =  when (targetState) {
        is RedirectPseudoState -> recursiveResolveTargetState(targetState.resolveTargetState(this))
        is HistoryState -> targetState.storedState
        is UndoState -> targetState.popState()
        else -> targetState
    }
    // when target state calculated we need to check if its entry will trigger another redirection
    // by initial child choiceState (for instance)
    return if (resolvedTarget != null) {
        val initialPseudoState = resolvedTarget.findInitialPseudoState()
        if (initialPseudoState == null) resolvedTarget else recursiveResolveTargetState(initialPseudoState)
    } else {
        null // means no transition
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

/**
 * Finds [PseudoState] if it is on initial path (would be activated if simply enter initial state path)
 */
private fun IState.findInitialPseudoState(): PseudoState? {
    if (this is PseudoState) return this
    if (states.isEmpty()) return null
    when (childMode) {
        ChildMode.EXCLUSIVE -> {
            val initialState = checkNotNull(initialState) {
                "Initial state is not set, call setInitialState() first"
            }
            return if (initialState !is StateMachine)  // inner state machine manages its internal state by its own
                initialState.findInitialPseudoState()
            else
                null
        }

        ChildMode.PARALLEL -> {
            val initialStates = states.mapNotNull {
                if (it !is StateMachine) // inner state machine manages its internal state by its own
                    it.findInitialPseudoState()
                else
                    null
            }
            return if (initialStates.isEmpty())
                null
            else
                initialStates.first() // fixme take first or other else??
        }
    }
}