package ru.nsk.kstatemachine.transition

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.findLca
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.state.pseudo.UndoState
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.transition.TransitionDirectionProducerPolicy.DefaultPolicy

sealed interface TransitionDirection {
    /**
     * Already resolved target state of conditional transition or [PseudoState] computation
     * This is always one of [targetStates] list elements or null, if the list is empty.
     */
    val targetState: IState? get() = null

    /**
     * Transition can target multiple states if they all are located at different regions of a parallel state.
     */
    val targetStates: Set<IState>
}

/**
 * [Transition] is triggered, but state is not changed
 */
internal object Stay : TransitionDirection {
    override val targetStates = emptySet<IState>()
}

fun stay(): TransitionDirection = Stay

/**
 * [Transition] should not be triggered
 */
internal object NoTransition : TransitionDirection {
    override val targetStates = emptySet<IState>()
}

fun noTransition(): TransitionDirection = NoTransition

/**
 * [Transition] is triggered with [targetStates] (usually with single [targetState])
 */
internal data class TargetState(
    override val targetStates: Set<IState>,
    override val targetState: IState = targetStates.first()
) : TransitionDirection {
    init {
        require(targetStates.contains(targetState)) {
            "Internal logical error, invalid ${TargetState::class.simpleName} construction, this should never happen"
        }
    }
}

/**
 * [Transition] is triggered with a targetState, resolving it in place if it is a [PseudoState]
 */
suspend fun EventAndArgument<*>.targetState(targetState: IState): TransitionDirection = resolveTargetState(targetState)

suspend fun EventAndArgument<*>.targetParallelStates(targetStates: Set<IState>): TransitionDirection {
    require(targetStates.size >= 2) {
        "There should be at least two targetStates, current amount ${targetStates.size}," +
                " check that you are not using the same state multiple times"
    }
    val resolvedStates = mutableSetOf<IState>()
    targetStates.mapNotNullTo(resolvedStates) { recursiveResolveTargetState(it) }
    if (resolvedStates.isEmpty()) return NoTransition

    @Suppress("UNCHECKED_CAST")
    val lca = findLca(resolvedStates as Set<InternalNode>) as InternalState
    check(lca.findParallelAncestor() != null) {
        "Resolved states does not have common ancestor with ${ChildMode.PARALLEL} child mode. " +
                "Only children of a state with ${ChildMode.PARALLEL} child mode" +
                " might be used as effective (resolved) targets here."
    }
    return TargetState(resolvedStates)
}

private fun InternalState.findParallelAncestor(): InternalState? {
    return if (childMode == ChildMode.PARALLEL) this else internalParent?.findParallelAncestor()
}

suspend fun EventAndArgument<*>.targetParallelStates(
    targetState1: IState,
    targetState2: IState,
    vararg targetStates: IState
) = targetParallelStates(setOf(targetState1, targetState2, *targetStates))

private suspend fun EventAndArgument<*>.resolveTargetState(targetState: IState): TransitionDirection {
    val resolvedState = recursiveResolveTargetState(targetState)
    return if (resolvedState != null) TargetState(setOf(resolvedState)) else NoTransition
}

private suspend fun EventAndArgument<*>.recursiveResolveTargetState(targetState: IState): IState? {
    val resolvedTarget = when (targetState) {
        // We can return here to optimize out double initialPseudoState resolution,
        // as initialPseudoState resolution is already done inside RedirectPseudoState::resolveTargetState()
        is RedirectPseudoState -> return targetState.resolveTargetState(DefaultPolicy(this)).targetState
        is HistoryState -> targetState.storedState
        is UndoState -> targetState.popState().firstOrNull() // fixme this is a bug, should use all set items, add test for undo multi-target transition
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

internal fun unresolvedTargetState(targetState: IState): TransitionDirection = TargetState(setOf(targetState))

/**
 * Transition that matches event and has a meaningful direction (except [NoTransition])
 */
typealias ResolvedTransition<E> = Pair<InternalTransition<E>, TransitionDirection>

internal typealias TransitionDirectionProducer<E> = suspend (TransitionDirectionProducerPolicy<E>) -> TransitionDirection

sealed class TransitionDirectionProducerPolicy<E : Event> {
    /**
     * Standard behaviour, calls all lambdas and resolves target states
     */
    internal class DefaultPolicy<E : Event>(val eventAndArgument: EventAndArgument<E>) :
        TransitionDirectionProducerPolicy<E>() {
        override suspend fun targetState(targetState: IState) = eventAndArgument.targetState(targetState)
        override suspend fun targetStateOrStay(targetState: IState?) = targetState?.let { targetState(it) } ?: stay()
    }

    /**
     * Does not call conditional lambdas, gets only non-conditional target states
     */
    internal class CollectTargetStatesPolicy<E : Event> :
        TransitionDirectionProducerPolicy<E>() {
        override suspend fun targetState(targetState: IState) = unresolvedTargetState(targetState)
        override suspend fun targetStateOrStay(targetState: IState?) = targetState?.let { targetState(it) } ?: stay()
    }

    /**
     * Calls lambdas to get unresolved target states,
     * this may fail in runtime depending on user defined lambda behaviour
     */
    internal class UnsafeCollectTargetStatesPolicy<E : Event>(val eventAndArgument: EventAndArgument<E>) :
        TransitionDirectionProducerPolicy<E>() {
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
            val initialState = requireInitialState()
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
                initialStates.first() // take first or other else?
        }
    }
}