package ru.nsk.kstatemachine

/**
 * Defines transition API for internal library usage. All transitions must implement this interface.
 */
interface InternalTransition<E : Event> : Transition<E> {
    override val sourceState: InternalState
    fun produceTargetStateDirection(policy: TransitionDirectionProducerPolicy<E>): TransitionDirection
}

internal fun InternalTransition<*>.transitionNotify(block: Transition.Listener.() -> Unit) =
    listeners.forEach {
        try {
            it.block()
        } catch (e: Exception) {
            val machine = sourceState.machine as InternalStateMachine
            machine.delayListenerException(e)
        }
    }