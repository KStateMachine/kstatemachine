package ru.nsk.kstatemachine

/**
 * Defines transition API for internal library usage. All transitions must implement this interface.
 */
interface InternalTransition<E : Event> : Transition<E> {
    override val sourceState: InternalState
    fun produceTargetStateDirection(policy: TransitionDirectionProducerPolicy<E>): TransitionDirection
}

internal inline fun InternalTransition<*>.transitionNotify(block: Transition.Listener.() -> Unit) {
    val machine = sourceState.machine as InternalStateMachine
    listeners.toList().forEach { machine.runDelayingException { it.block() } }
}