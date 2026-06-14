/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2026.
 * All rights reserved.
 */

@file:OptIn(ExperimentalContracts::class)

package ru.nsk.kstatemachine.state

import ru.nsk.kstatemachine.event.AutoDataEvent
import ru.nsk.kstatemachine.event.AutoDataEventImpl
import ru.nsk.kstatemachine.event.AutoEvent
import ru.nsk.kstatemachine.event.AutoEventImpl
import ru.nsk.kstatemachine.event.autoDataEventMatcher
import ru.nsk.kstatemachine.event.autoEventMatcher
import ru.nsk.kstatemachine.metainfo.MetaInfo
import ru.nsk.kstatemachine.transition.AutoDataGuardedTransitionBuilder
import ru.nsk.kstatemachine.transition.AutoDataGuardedTransitionOnBuilder
import ru.nsk.kstatemachine.transition.ConditionalTransitionBuilder
import ru.nsk.kstatemachine.transition.DefaultTransition
import ru.nsk.kstatemachine.transition.Transition
import ru.nsk.kstatemachine.transition.TransitionDirection
import ru.nsk.kstatemachine.transition.TransitionParams
import ru.nsk.kstatemachine.transition.TransitionType
import ru.nsk.kstatemachine.transition.TransitionType.LOCAL
import ru.nsk.kstatemachine.transition.UnitGuardedTransitionBuilder
import ru.nsk.kstatemachine.transition.UnitGuardedTransitionOnBuilder
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Adds a UML eventless ("always") transition to this state.
 *
 * The transition fires automatically when the source state is entered — no explicit event is
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
fun TransitionStateApi.autoTransition(
    name: String? = null,
    targetState: State? = null,
    type: TransitionType = LOCAL,
    metaInfo: MetaInfo? = null,
): Transition<AutoEvent> {
    val transitionId = Any()
    installAutoEventTrigger(transitionId)
    return addTransition(
        DefaultTransition(
            name = name,
            eventMatcher = autoEventMatcher(transitionId),
            type = type,
            metaInfo = metaInfo,
            sourceState = asState(),
            targetState = targetState,
        )
    )
}

/**
 * Creates transition.
 * You can specify guard function. Such guarded transition is triggered only when guard function returns true.
 *
 * This is a special kind of conditional transition but with simpler syntax and less flexibility.
 */
fun TransitionStateApi.autoTransition(
    name: String? = null,
    block: UnitGuardedTransitionBuilder<AutoEvent>.() -> Unit
): Transition<AutoEvent> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val transitionId = Any()
    val builder = UnitGuardedTransitionBuilder<AutoEvent>(name, asState()).apply {
        block()
        eventMatcher = autoEventMatcher(transitionId) // after block(), so user can't change it
    }
    installAutoEventTrigger(transitionId)
    return addTransition(builder.build())
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
fun TransitionStateApi.autoTransitionOn(
    name: String? = null,
    block: UnitGuardedTransitionOnBuilder<AutoEvent>.() -> Unit
): Transition<AutoEvent> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val transitionId = Any()
    val builder = UnitGuardedTransitionOnBuilder<AutoEvent>(name, asState()).apply {
        block()
        eventMatcher = autoEventMatcher(transitionId) // after block(), so user can't change it
    }
    installAutoEventTrigger(transitionId)
    return addTransition(builder.build())
}

/**
 * Eventless transition with full conditional control over the [TransitionDirection].
 */
fun TransitionStateApi.autoTransitionConditionally(
    name: String? = null,
    block: ConditionalTransitionBuilder<AutoEvent>.() -> Unit
): Transition<AutoEvent> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val transitionId = Any()
    val builder = ConditionalTransitionBuilder<AutoEvent>(name, asState()).apply {
        block()
        eventMatcher = autoEventMatcher(transitionId) // after block(), so user can't change it
    }
    installAutoEventTrigger(transitionId)
    return addTransition(builder.build())
}

/**
 * Type-safe eventless transition that targets a [DataState].
 *
 * [dataProducer] is invoked once per firing (at state entry) to produce the data for [targetState],
 * exactly as `joinDataTransition`'s producer does. The resulting [AutoDataEventImpl] is a
 * `DataEvent<D>`, so `DefaultDataState.onDoEnter` consumes it through its existing data branch —
 * no custom `DataExtractor` needed.
 */
fun <D : Any> TransitionStateApi.autoDataTransition(
    name: String? = null,
    targetState: DataState<D>,
    type: TransitionType = LOCAL,
    metaInfo: MetaInfo? = null,
    dataProducer: suspend () -> D
): Transition<AutoDataEvent<D>> {
    val transitionId = Any()
    installAutoDataEventTrigger(transitionId, dataProducer)
    return addTransition(
        DefaultTransition(
            name = name,
            eventMatcher = autoDataEventMatcher(transitionId),
            type = type,
            metaInfo = metaInfo,
            sourceState = asState(),
            targetState = targetState,
        )
    )
}

/**
 * Shortcut function for type safe target-less (self targeted) transition.
 */
fun <D : Any> DataTransitionStateApi<D>.autoDataTransition(
    name: String? = null,
    type: TransitionType = LOCAL,
    metaInfo: MetaInfo? = null,
    dataProducer: suspend () -> D
): Transition<AutoDataEvent<D>> {
    val transitionId = Any()
    installAutoDataEventTrigger(transitionId, dataProducer)
    return addTransition(
        DefaultTransition(
            name = name,
            eventMatcher = autoDataEventMatcher(transitionId),
            type = type,
            metaInfo = metaInfo,
            sourceState = asState(),
            targetState = null,
        )
    )
}

fun <D : Any> TransitionStateApi.autoDataTransition(
    name: String? = null,
    block: AutoDataGuardedTransitionBuilder<D>.() -> Unit
): Transition<AutoDataEvent<D>> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val transitionId = Any()
    val builder = AutoDataGuardedTransitionBuilder<D>(name, asState(), transitionId)
    builder.block()

    installAutoDataEventTrigger(transitionId, builder.dataProducer)
    return addTransition(builder.build())
}

/**
 * Data transition, otherwise same as [transitionOn]
 */
fun <D : Any> TransitionStateApi.autoDataTransitionOn(
    name: String? = null,
    block: AutoDataGuardedTransitionOnBuilder<D>.() -> Unit
): Transition<AutoDataEvent<D>> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val transitionId = Any()
    val builder = AutoDataGuardedTransitionOnBuilder<D>(name, asState(), transitionId)
    builder.block()

    installAutoDataEventTrigger(transitionId, builder.dataProducer)
    return addTransition(builder.build())
}


/**
 * Installs the `onEntry` listener that fires the [AutoEventImpl] for the non-data variants.
 */
private fun <D : Any> TransitionStateApi.installAutoDataEventTrigger(
    transitionId: Any,
    dataProducer: suspend () -> D
) {
    asState().addListener(object : IState.Listener {
        override suspend fun onEntry(transitionParams: TransitionParams<*>) {
            asState().machine.processEvent(AutoDataEventImpl(transitionId, dataProducer()))
        }
    })
}

/**
 * Installs the `onEntry` listener that fires the [AutoEventImpl] for the non-data variants.
 */
private fun TransitionStateApi.installAutoEventTrigger(transitionId: Any) {
    asState().addListener(object : IState.Listener {
        override suspend fun onEntry(transitionParams: TransitionParams<*>) {
            asState().machine.processEvent(AutoEventImpl(transitionId))
        }
    })
}
