package ru.nsk.kstatemachine

/**
 * Defines state API for internal library usage. All states must implement this interface.
 */
interface InternalState : State {
    override var parent: InternalState?

    fun stateNotify(block: State.Listener.() -> Unit)

    fun doEnter(transitionParams: TransitionParams<*>)
    fun doExit(transitionParams: TransitionParams<*>)

    fun recursiveEnterInitialState()
    fun recursiveEnterStatePath(path: MutableList<InternalState>, transitionParams: TransitionParams<*>)
    fun recursiveExit(transitionParams: TransitionParams<*>)

    /** @return true if event was processed */
    fun recursiveProcessEvent(event: Event, argument: Any?): Boolean
}

internal fun InternalState.isNeighbor(state: State) = parent?.states?.contains(state) == true

internal fun InternalState.requireParent() = requireNotNull(parent) { "Parent is not set" }

internal fun <E : Event> InternalState.findTransitionsByEvent(event: E): List<InternalTransition<E>> {
    val triggeringTransitions = transitions.filter { it.isTriggeringEvent(event) }
    @Suppress("UNCHECKED_CAST")
    return triggeringTransitions as List<InternalTransition<E>>
}

internal fun <E : Event> InternalState.findUniqueTransitionWithDirection(event: E)
        : Pair<InternalTransition<E>, TransitionDirection>? {
    val transitions = findTransitionsByEvent(event)
        .map { it to it.produceTargetStateDirection() }
        .filter { it.second !is NoTransition }
    check(transitions.size <= 1) { "Multiple transitions match $event $transitions in $this" }
    return transitions.firstOrNull()
}

