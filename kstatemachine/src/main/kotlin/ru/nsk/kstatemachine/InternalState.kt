package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.TransitionDirectionProducerPolicy.DefaultPolicy

/**
 * Defines state API for internal library usage. All states must implement this interface.
 */
interface InternalState : IState {
    override var parent: InternalState?

    fun doEnter(transitionParams: TransitionParams<*>)
    fun doExit(transitionParams: TransitionParams<*>)

    /** @return true if event was processed */
    fun doProcessEvent(event: Event, argument: Any?): Boolean

    fun <E : Event> recursiveFindUniqueResolvedTransition(event: E): ResolvedTransition<E>?
    fun recursiveEnterInitialStates()
    fun recursiveEnterStatePath(path: MutableList<InternalState>, transitionParams: TransitionParams<*>)
    fun recursiveExit(transitionParams: TransitionParams<*>)
    fun recursiveStop()
    fun recursiveFillActiveStates(states: MutableSet<IState>)
}

internal fun InternalState.isNeighbor(state: IState) = parent?.states?.contains(state) == true

internal fun InternalState.requireParent() = requireNotNull(parent) { "Parent is not set" }

internal fun InternalState.stateNotify(block: IState.Listener.() -> Unit) = listeners.forEach { it.apply(block) }

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
