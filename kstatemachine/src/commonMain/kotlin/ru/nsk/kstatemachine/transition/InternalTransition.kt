/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.transition

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.InternalState

/**
 * Defines transition API for internal library usage. All transitions must implement this interface.
 * This is safe to cast any [Transition] to [InternalTransition] by design.
 */
interface InternalTransition<E : Event> : Transition<E> {
    override val sourceState: InternalState
    suspend fun produceTargetStateDirection(policy: TransitionDirectionProducerPolicy<E>): TransitionDirection
}