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

    internal abstract fun doEnter(transitionParams: TransitionParams<*>)
    internal abstract fun doExit(transitionParams: TransitionParams<*>)
    internal abstract fun afterChildFinished(finishedChild: InternalState, transitionParams: TransitionParams<*>)

    internal abstract fun <E : Event> recursiveFindUniqueResolvedTransition(event: E): ResolvedTransition<E>?
    internal abstract fun recursiveEnterInitialStates()
    internal abstract fun recursiveEnterStatePath(
        path: MutableList<InternalState>,
        transitionParams: TransitionParams<*>
    )

    internal abstract fun recursiveExit(transitionParams: TransitionParams<*>)
    internal abstract fun recursiveStop()
}

internal fun InternalState.isNeighbor(state: IState) = parent?.states?.contains(state) == true

internal fun InternalState.requireParent() = requireNotNull(internalParent) { "Parent is not set" }

internal fun InternalState.stateNotify(block: IState.Listener.() -> Unit) = listeners.forEach(block)

internal fun <E : Event> InternalState.findTransitionsByEvent(event: E): List<InternalTransition<E>> {
    val triggeringTransitions = transitions.filter { it.isMatchingEvent(event) }
    @Suppress("UNCHECKED_CAST")
    return triggeringTransitions as List<InternalTransition<E>>
}

internal fun <E : Event> InternalState.findUniqueResolvedTransition(event: E): ResolvedTransition<E>? {
    val policy = DefaultPolicy(event)
    val transitions = findTransitionsByEvent(event)
        .map { it to it.produceTargetStateDirection(policy) }
        .filter { it.second !is NoTransition }
    check(transitions.size <= 1) { "Multiple transitions match $event, $transitions in $this" }
    return transitions.singleOrNull()
}
