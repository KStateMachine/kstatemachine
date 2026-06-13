/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

@file:OptIn(ExperimentalContracts::class)

package ru.nsk.kstatemachine.state

import ru.nsk.kstatemachine.event.AutoDataEventImpl
import ru.nsk.kstatemachine.event.AutoEvent
import ru.nsk.kstatemachine.event.AutoEventImpl
import ru.nsk.kstatemachine.event.autoEventMatcher
import ru.nsk.kstatemachine.transition.ConditionalTransitionBuilder
import ru.nsk.kstatemachine.transition.DefaultTransition
import ru.nsk.kstatemachine.transition.Transition
import ru.nsk.kstatemachine.transition.TransitionParams
import ru.nsk.kstatemachine.transition.TransitionType
import ru.nsk.kstatemachine.transition.UnitGuardedTransitionBuilder
import ru.nsk.kstatemachine.transition.UnitGuardedTransitionOnBuilder
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Adds a UML eventless ("always") transition to this state.
 *
 * The transition fires automatically when the source state is entered — no external event is
 * required. After it lands in the target state, that state's own eventless transitions (if any)
 * are evaluated in turn, producing UML run-to-completion semantics.
 *
 * Guards are evaluated at fire time. If the guard rejects, the state simply stays put — there is
 * no automatic re-evaluation while the state is active. The transition will be re-tried only on
 * the next entry.
 *
 * Implementation note: this is a `processEvent` of an internal [AutoEvent] from the source state's
 * `onEntry` listener. The library's `QueuePendingEventHandler` drains chained auto-events.
 */
fun IState.automaticTransition(
    name: String? = null,
    targetState: State,
): Transition<AutoEvent> {
    val transitionId = Any()
    val sourceState = this
    sourceState.installAutoEventTrigger(transitionId)
    return sourceState.addTransition(
        DefaultTransition(
            name = name,
            eventMatcher = autoEventMatcher(transitionId),
            type = TransitionType.LOCAL,
            metaInfo = null,
            sourceState = sourceState,
            targetState = targetState,
        )
    )
}

/**
 * Eventless transition with a guard and/or lazy target lookup.
 *
 * Example:
 * ```
 * automaticTransitionOn {
 *     guard = { ready }
 *     targetState = { nextState }
 * }
 * ```
 */
fun IState.automaticTransitionOn(
    name: String? = null,
    block: UnitGuardedTransitionOnBuilder<AutoEvent>.() -> Unit,
): Transition<AutoEvent> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val transitionId = Any()
    val sourceState = this
    val builder = UnitGuardedTransitionOnBuilder<AutoEvent>(name, sourceState).apply {
        eventMatcher = autoEventMatcher(transitionId)
        block()
    }
    sourceState.installAutoEventTrigger(transitionId)
    return sourceState.addTransition(builder.build())
}

/**
 * Eventless transition with full conditional control over the [TransitionDirection].
 */
fun IState.automaticTransitionConditionally(
    name: String? = null,
    block: ConditionalTransitionBuilder<AutoEvent>.() -> Unit,
): Transition<AutoEvent> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val transitionId = Any()
    val sourceState = this
    val builder = ConditionalTransitionBuilder<AutoEvent>(name, sourceState).apply {
        eventMatcher = autoEventMatcher(transitionId)
        block()
    }
    sourceState.installAutoEventTrigger(transitionId)
    return sourceState.addTransition(builder.build())
}

/**
 * Type-safe eventless transition that targets a [DataState].
 *
 * [dataProducer] is invoked once per firing (at state entry) to produce the data for [targetState],
 * exactly as `joinDataTransition`'s producer does. The resulting [AutoDataEventImpl] is a
 * `DataEvent<D>`, so `DefaultDataState.onDoEnter` consumes it through its existing data branch —
 * no custom `DataExtractor` needed.
 */
fun <D : Any> IState.automaticDataTransition(
    name: String? = null,
    targetState: DataState<D>,
    dataProducer: suspend () -> D,
): Transition<AutoEvent> {
    val transitionId = Any()
    val sourceState = this
    sourceState.addListener(object : IState.Listener {
        override suspend fun onEntry(transitionParams: TransitionParams<*>) {
            sourceState.machine.processEvent(AutoDataEventImpl(transitionId, dataProducer()))
        }
    })
    return sourceState.addTransition(
        DefaultTransition(
            name = name,
            eventMatcher = autoEventMatcher(transitionId),
            type = TransitionType.LOCAL,
            metaInfo = null,
            sourceState = sourceState,
            targetState = targetState,
        )
    )
}

/**
 * Installs the `onEntry` listener that fires the [AutoEventImpl] for the non-data variants.
 * Mirrors the listener pattern in `JoinTransition.kt:58-66`.
 */
private fun IState.installAutoEventTrigger(transitionId: Any) {
    val sourceState = this
    sourceState.addListener(object : IState.Listener {
        override suspend fun onEntry(transitionParams: TransitionParams<*>) {
            sourceState.machine.processEvent(AutoEventImpl(transitionId))
        }
    })
}
