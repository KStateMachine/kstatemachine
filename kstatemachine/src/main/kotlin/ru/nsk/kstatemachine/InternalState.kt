package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.TransitionDirectionProducerPolicy.DefaultPolicy

/**
 * Defines state API for internal library usage. All states must implement this class.
 * Unfortunately cannot use interface for this purpose.
 */
abstract class InternalState : IState {
    override val parent: IState? get() = internalParent
    internal abstract val internalParent: InternalState?
    internal abstract fun setParent(parent: InternalState)

    internal abstract fun getCurrentStates(): List<InternalState>

    internal abstract suspend fun doEnter(transitionParams: TransitionParams<*>)
    internal abstract suspend fun doExit(transitionParams: TransitionParams<*>)
    internal abstract suspend fun afterChildFinished(finishedChild: InternalState, transitionParams: TransitionParams<*>)
    internal open fun onParentCurrentStateChanged(currentState: InternalState) = Unit

    internal abstract suspend fun <E : Event> recursiveFindUniqueResolvedTransition(
        eventAndArgument: EventAndArgument<E>
    ): ResolvedTransition<E>?

    internal abstract suspend fun recursiveEnterInitialStates(transitionParams: TransitionParams<*>)
    internal abstract suspend fun recursiveEnterStatePath(
        path: MutableList<InternalState>,
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

internal fun InternalState.requireInternalParent() = requireNotNull(internalParent) { "$this parent is not set" }

internal suspend fun <E : Event> InternalState.findTransitionsByEvent(event: E): List<InternalTransition<E>> {
    val triggeringTransitions = transitions.filter { it.isMatchingEvent(event) }
    @Suppress("UNCHECKED_CAST")
    return triggeringTransitions as List<InternalTransition<E>>
}

internal suspend fun <E : Event> InternalState.findUniqueResolvedTransition(eventAndArgument: EventAndArgument<E>): ResolvedTransition<E>? {
    val policy = DefaultPolicy(eventAndArgument)
    val transitions = findTransitionsByEvent(eventAndArgument.event)
        .map { it to it.produceTargetStateDirection(policy) }
        .filter { it.second !is NoTransition }
    return if (!machine.doNotThrowOnMultipleTransitionsMatch) {
        check(transitions.size <= 1) { "Multiple transitions match ${eventAndArgument.event}, $transitions in $this" }
        transitions.singleOrNull()
    } else {
        transitions.firstOrNull()
    }
}