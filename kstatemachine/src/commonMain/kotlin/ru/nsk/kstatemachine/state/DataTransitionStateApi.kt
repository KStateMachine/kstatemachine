/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2026.
 * All rights reserved.
 */

@file:OptIn(ExperimentalContracts::class)

package ru.nsk.kstatemachine.state

import ru.nsk.kstatemachine.event.*
import ru.nsk.kstatemachine.metainfo.MetaInfo
import ru.nsk.kstatemachine.transition.*
import ru.nsk.kstatemachine.transition.TransitionType.LOCAL
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Same as [TransitionStateApi] interface, for specialized [DataState] api.
 */
interface DataTransitionStateApi<D : Any> : TransitionStateApi

/**
 * Shortcut function for type safe argument transition.
 * Data transition can be target-less (self-targeted), it is useful to update [DataState] data
 * Note that transition must be [TransitionType.EXTERNAL] to update data.
 */
inline fun <reified E : DataEvent<D>, D : Any> TransitionStateApi.dataTransition(
    name: String? = null,
    targetState: DataState<D>,
    type: TransitionType = LOCAL,
    metaInfo: MetaInfo? = null,
): Transition<E> {
    return addTransition(DefaultTransition(name, matcherForEvent(asState()), type, metaInfo, asState(), targetState))
}

/**
 * Shortcut function for type safe target-less (self targeted) transition.
 */
inline fun <reified E : DataEvent<D>, D : Any> DataTransitionStateApi<D>.dataTransition(
    name: String? = null,
    type: TransitionType = LOCAL,
    metaInfo: MetaInfo? = null,
): Transition<E> {
    return addTransition(DefaultTransition(name, matcherForEvent(asState()), type, metaInfo, asState(), null))
}

/**
 * Creates type safe argument transition to [DataState].
 */
inline fun <reified E : DataEvent<D>, D : Any> TransitionStateApi.dataTransition(
    name: String? = null,
    block: DataGuardedTransitionBuilder<E, D>.() -> Unit,
): Transition<E> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val builder = DataGuardedTransitionBuilder<E, D>(name, asState()).apply {
        eventMatcher = matcherForEvent(asState())
        block()
    }
    return addTransition(builder.build())
}

/**
 * Data transition, otherwise same as [transitionOn]
 */
inline fun <reified E : DataEvent<D>, D : Any> TransitionStateApi.dataTransitionOn(
    name: String? = null,
    block: DataGuardedTransitionOnBuilder<E, D>.() -> Unit,
): Transition<E> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val builder = DataGuardedTransitionOnBuilder<E, D>(name, asState()).apply {
        eventMatcher = matcherForEvent(asState())
        block()
    }
    return addTransition(builder.build())
}
