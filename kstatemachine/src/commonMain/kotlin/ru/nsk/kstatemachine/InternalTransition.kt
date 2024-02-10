package ru.nsk.kstatemachine

/**
 * Defines transition API for internal library usage. All transitions must implement this interface.
 * This is safe to cast any [Transition] to [InternalTransition] by design.
 */
interface InternalTransition<E : Event> : Transition<E> {
    override val sourceState: InternalState
    suspend fun produceTargetStateDirection(policy: TransitionDirectionProducerPolicy<E>): TransitionDirection
}