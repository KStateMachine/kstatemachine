package ru.nsk.kstatemachine.state

import ru.nsk.kstatemachine.PathNode
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.transition.*
import ru.nsk.kstatemachine.transition.TransitionDirectionProducerPolicy.DefaultPolicy

/**
 * Contains tree composition api for [InternalState].
 * Necessary for tree algorithms testing purpose, to be able not create full states in tests.
 * This is safe to cast any [InternalNode] to [InternalState] by design.
 */
internal interface InternalNode {
    val internalParent: InternalNode?
}

internal fun InternalNode.requireParentNode(): InternalNode =
    requireNotNull(internalParent) { "$this parent is not set" }

/**
 * Defines state API for internal library usage. All states must implement this class.
 * Unfortunately cannot use interface for this purpose.
 * This is safe to cast any [IState] to [InternalState] by design.
 */
abstract class InternalState : IState, InternalNode {
    override val parent: IState? get() = internalParent
    abstract override val internalParent: InternalState?
    internal abstract fun setParent(parent: InternalState)

    internal abstract fun getCurrentStates(): List<InternalState>

    internal abstract suspend fun doEnter(transitionParams: TransitionParams<*>)
    internal abstract suspend fun doExit(transitionParams: TransitionParams<*>)
    internal abstract suspend fun afterChildFinished(
        finishedChild: InternalState,
        transitionParams: TransitionParams<*>
    )

    internal open fun onParentCurrentStateChanged(currentState: InternalState) = Unit

    internal abstract suspend fun <E : Event> recursiveFindUniqueResolvedTransition(
        eventAndArgument: EventAndArgument<E>
    ): ResolvedTransition<E>?

    internal abstract suspend fun recursiveEnterInitialStates(transitionParams: TransitionParams<*>)

    /** Enters single branch path */
    internal abstract suspend fun recursiveEnterStatePath(
        path: ListIterator<InternalState>,
        transitionParams: TransitionParams<*>
    )

    /** Enters path with multiple branches */
    internal abstract suspend fun recursiveEnterStatePath(
        pathHead: PathNode,
        transitionParams: TransitionParams<*>
    )

    internal abstract suspend fun recursiveExit(transitionParams: TransitionParams<*>)
    internal abstract suspend fun recursiveStop()

    /**
     * Called after each (including initial) transition completion.
     */
    internal abstract suspend fun recursiveAfterTransitionComplete(transitionParams: TransitionParams<*>)
    internal abstract suspend fun cleanup()
}

internal fun InternalState.requireInternalParent(): InternalState =
    requireNotNull(internalParent) { "$this parent is not set" }

internal suspend fun <E : Event> InternalState.findTransitionsByEvent(event: E): List<InternalTransition<E>> {
    @Suppress("UNCHECKED_CAST")
    return transitions.filter { it.isMatchingEvent(event) } as List<InternalTransition<E>>
}

internal suspend fun <E : Event> InternalState.findUniqueResolvedTransition(eventAndArgument: EventAndArgument<E>): ResolvedTransition<E>? {
    val policy = DefaultPolicy(eventAndArgument)
    val transitions = findTransitionsByEvent(eventAndArgument.event)
        .map { it to it.produceTargetStateDirection(policy) }
        .filter { it.second !is NoTransition }
    return if (!machine.creationArguments.doNotThrowOnMultipleTransitionsMatch) {
        check(transitions.size <= 1) { "Multiple transitions match ${eventAndArgument.event}, $transitions in $this" }
        transitions.singleOrNull()
    } else {
        transitions.firstOrNull()
    }
}