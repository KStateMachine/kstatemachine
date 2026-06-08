/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.transition

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.findLca
import ru.nsk.kstatemachine.state.ChildMode
import ru.nsk.kstatemachine.state.HistoryState
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.state.InternalNode
import ru.nsk.kstatemachine.state.InternalState
import ru.nsk.kstatemachine.state.PseudoState
import ru.nsk.kstatemachine.state.RedirectPseudoState
import ru.nsk.kstatemachine.state.pseudo.UndoState
import ru.nsk.kstatemachine.state.requireInitialState
import ru.nsk.kstatemachine.state.transitionConditionally
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.transition.TransitionDirectionProducerPolicy.DefaultPolicy

/**
 * Caller should check subclass to recognise/distinguish [NoTransition] and [Stay] cases.
 */
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

/**
 * Transitions the machine to multiple [targetStates] simultaneously, activating one state per region of a
 * [ChildMode.PARALLEL] parent. This is the programmatic equivalent of a **UML fork pseudo-state**: a single
 * transition splits control into several concurrent orthogonal regions.
 *
 * Each element of [targetStates] is resolved before use: [PseudoState] targets (e.g. choice or history states)
 * are followed to their effective destination, so you may pass a [PseudoState] as a target and it will be
 * transparently resolved at runtime.
 *
 * Constraints:
 * - At least two target states must be provided.
 * - After resolution, every effective target must reside inside a [ChildMode.PARALLEL] parent (i.e. they must all
 *   be in distinct orthogonal regions of the same parallel state).
 *
 * Typical usage inside [transitionConditionally]:
 * ```kotlin
 * redState {
 *     transitionConditionally<SwitchEvent> {
 *         direction = { targetParallelStates(region1State, region2State) }
 *     }
 * }
 * ```
 */
suspend fun EventAndArgument<*>.targetParallelStates(targetStates: Set<IState>): TransitionDirection {
    require(targetStates.size >= 2) {
        "There should be at least two targetStates, current amount ${targetStates.size}," +
                " check that you are not using the same state multiple times"
    }
    val resolvedStates = mutableSetOf<IState>()
    targetStates.forEach { resolvedStates.addAll(recursiveResolveTargetStates(it) ?: return@forEach) }
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

/** Convenience overload of [targetParallelStates] that accepts individual state arguments instead of a [Set]. */
suspend fun EventAndArgument<*>.targetParallelStates(
    targetState1: IState,
    targetState2: IState,
    vararg targetStates: IState
) = targetParallelStates(setOf(targetState1, targetState2, *targetStates))

private suspend fun EventAndArgument<*>.resolveTargetState(targetState: IState): TransitionDirection {
    val resolvedStates = recursiveResolveTargetStates(targetState)
    return if (resolvedStates != null) TargetState(resolvedStates) else NoTransition
}

/**
 * @return resolved target states or null, which means no transition.
 * Usually returns a single-element set, but may return multiple states when parallel initial pseudo-states
 * each redirect to their own sub-state within the same parallel structure.
 */
private suspend fun EventAndArgument<*>.recursiveResolveTargetStates(targetState: IState): Set<IState>? {
    val resolvedTarget = when (targetState) {
        // We can return here to optimize out double initialPseudoState resolution,
        // as initialPseudoState resolution is already done inside RedirectPseudoState::resolveTargetState()
        is RedirectPseudoState -> return targetState.resolveTargetState(DefaultPolicy(this)).targetStates
        is HistoryState -> targetState.storedState
        is UndoState -> {
            val undoTargets = targetState.popTargetStates()
            when {
                undoTargets.isEmpty() -> return null
                undoTargets.size == 1 -> undoTargets.single()
                else -> {
                    // Multiple targets (from a parallel targetParallelStates transition) — resolve each and combine
                    val allResolved = undoTargets.flatMapTo(mutableSetOf()) {
                        recursiveResolveTargetStates(it) ?: emptySet()
                    }
                    return allResolved.ifEmpty { null }
                }
            }
        }
        else -> targetState
    }
    // when target state calculated we need to check if its entry will trigger another redirection
    // by initial child choiceState (for instance)
    val initialPseudoStates = resolvedTarget.findAllInitialPseudoStates()
    return when (initialPseudoStates.size) {
        0 -> setOf(resolvedTarget)
        1 -> recursiveResolveTargetStates(initialPseudoStates.single())
        else -> {
            // Multiple pseudo-states in parallel regions: resolve each independently
            val allTargets = initialPseudoStates.flatMapTo(mutableSetOf()) {
                recursiveResolveTargetStates(it) ?: emptySet()
            }
            if (allTargets.isEmpty()) return null
            // If all resolved targets share a PARALLEL ancestor they are compatible (each in its own region)
            @Suppress("UNCHECKED_CAST")
            val lca = findLca(allTargets as Set<InternalNode>) as InternalState
            check(lca.findParallelAncestor() != null) {
                val parallelState = (initialPseudoStates.first() as InternalState).findParallelAncestor()
                    ?: resolvedTarget
                "multiple transitions match: multiple initial pseudo-states found in $parallelState"
            }
            allTargets
        }
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
 * Finds all [PseudoState]s on the initial path (states that would be activated if simply entering the initial state path).
 * Returns one element for EXCLUSIVE children, potentially multiple for PARALLEL when each region has an initial pseudo-state.
 */
private fun IState.findAllInitialPseudoStates(): List<PseudoState> {
    if (this is PseudoState) return listOf(this)
    if (states.isEmpty()) return emptyList()
    when (childMode) {
        ChildMode.EXCLUSIVE -> {
            val initialState = requireInitialState()
            return if (initialState !is StateMachine)  // inner state machine manages its internal state by its own
                initialState.findAllInitialPseudoStates()
            else
                emptyList()
        }

        ChildMode.PARALLEL -> {
            return states.flatMap {
                if (it !is StateMachine) // inner state machine manages its internal state by its own
                    it.findAllInitialPseudoStates()
                else
                    emptyList()
            }
        }
    }
}