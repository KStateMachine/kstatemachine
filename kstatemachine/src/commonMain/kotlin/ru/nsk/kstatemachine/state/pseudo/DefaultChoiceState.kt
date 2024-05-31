/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state.pseudo

import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.transition.EventAndArgument
import ru.nsk.kstatemachine.transition.TransitionDirection
import ru.nsk.kstatemachine.transition.TransitionDirectionProducerPolicy
import ru.nsk.kstatemachine.transition.noTransition
import kotlin.reflect.KClass

open class DefaultChoiceState(
    name: String? = null,
    private val choiceAction: suspend EventAndArgument<*>.() -> State
) : BasePseudoState(name), RedirectPseudoState {

    override suspend fun resolveTargetState(policy: TransitionDirectionProducerPolicy<*>): TransitionDirection {
        return internalResolveTargetState(policy, choiceAction)
    }
}

open class DefaultChoiceDataState<D : Any>(
    name: String? = null,
    override val dataClass: KClass<D>,
    private val choiceAction: suspend EventAndArgument<*>.() -> DataState<D>,
) : DataState<D>, BasePseudoState(name), RedirectPseudoState {

    override suspend fun resolveTargetState(policy: TransitionDirectionProducerPolicy<*>): TransitionDirection {
        return internalResolveTargetState(policy, choiceAction)
    }

    override val defaultData: D? = null
    override val data: D get() = error("PseudoState $this can not have data")
    override val lastData: D get() = error("PseudoState $this can not have lastData")
}

private suspend fun IState.internalResolveTargetState(
    policy: TransitionDirectionProducerPolicy<*>,
    choiceAction: suspend EventAndArgument<*>.() -> IState
): TransitionDirection {
    return when (policy) {
        is TransitionDirectionProducerPolicy.DefaultPolicy -> policy.targetState(
            policy.eventAndArgument.choiceAction().also { log { "$this resolved to $it" } }
        )
        is TransitionDirectionProducerPolicy.CollectTargetStatesPolicy -> noTransition()
        is TransitionDirectionProducerPolicy.UnsafeCollectTargetStatesPolicy -> policy.targetState(policy.eventAndArgument.choiceAction())
    }
}