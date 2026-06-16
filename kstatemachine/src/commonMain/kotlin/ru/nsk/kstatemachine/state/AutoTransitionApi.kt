/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2026.
 * All rights reserved.
 */

@file:OptIn(ExperimentalContracts::class)

package ru.nsk.kstatemachine.state

import ru.nsk.kstatemachine.coroutines.Cancellable
import ru.nsk.kstatemachine.event.AutoDataEvent
import ru.nsk.kstatemachine.event.AutoDataEventImpl
import ru.nsk.kstatemachine.event.AutoEvent
import ru.nsk.kstatemachine.event.AutoEventImpl
import ru.nsk.kstatemachine.event.autoEventMatcher
import ru.nsk.kstatemachine.metainfo.MetaInfo
import ru.nsk.kstatemachine.metainfo.plus
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.transition.AutoConditionalTransitionBuilder
import ru.nsk.kstatemachine.transition.AutoDataGuardedTransitionBuilder
import ru.nsk.kstatemachine.transition.AutoDataGuardedTransitionOnBuilder
import ru.nsk.kstatemachine.transition.AutoUnitGuardedTransitionBuilder
import ru.nsk.kstatemachine.transition.AutoUnitGuardedTransitionOnBuilder
import ru.nsk.kstatemachine.transition.DefaultTransition
import ru.nsk.kstatemachine.transition.Transition
import ru.nsk.kstatemachine.transition.TransitionParams
import ru.nsk.kstatemachine.transition.TransitionType
import ru.nsk.kstatemachine.transition.TransitionType.LOCAL
import ru.nsk.kstatemachine.transition.delayedMetaInfoOrNull
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration

/**
 * Adds a UML eventless ("always") transition to this state, with an optional timer delay.
 *
 * When [delay] is `null` (default): fires immediately on source-state entry. No coroutines
 * required — works with `createStdLibStateMachine`.
 *
 * When [delay] is non-null: fires after the delay elapses (UML time-event). Requires a machine
 * created with `createStateMachine` (coroutines support); throws otherwise.
 *
 * Guards are evaluated at fire time. If the guard rejects, the state stays put — the transition
 * is re-tried only on the next entry (delayed: timer does NOT auto-restart).
 */
fun TransitionStateApi.autoTransition(
    name: String? = null,
    delay: Duration? = null,
    targetState: State? = null,
    type: TransitionType = LOCAL,
    metaInfo: MetaInfo? = null,
): Transition<AutoEvent> {
    val transitionId = Any()
    installAutoTrigger(delay) { AutoEventImpl(transitionId) }
    return addTransition(
        DefaultTransition(
            name = name,
            eventMatcher = autoEventMatcher(transitionId),
            type = type,
            metaInfo = metaInfo + delayedMetaInfoOrNull(delay),
            sourceState = asState(),
            targetState = targetState,
        )
    )
}

/**
 * Scoped variant — set [AutoUnitGuardedTransitionBuilder.delay], `guard`, and `targetState` inside
 * the lambda.
 *
 * Example:
 * ```
 * autoTransition {
 *     delay = 2.seconds   // omit for immediate (eventless) behavior
 *     guard = { ready }
 *     targetState = nextState
 * }
 * ```
 */
fun TransitionStateApi.autoTransition(
    name: String? = null,
    block: AutoUnitGuardedTransitionBuilder.() -> Unit,
): Transition<AutoEvent> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val transitionId = Any()
    val builder = AutoUnitGuardedTransitionBuilder(name, asState(), transitionId)
    builder.block()

    installAutoTrigger(builder.delay) { AutoEventImpl(transitionId) }
    return addTransition(builder.build())
}

/**
 * Scoped variant with a lazy target lookup — set [delay], `guard`, and `targetState` lambda inside
 * the builder block.
 */
fun TransitionStateApi.autoTransitionOn(
    name: String? = null,
    block: AutoUnitGuardedTransitionOnBuilder.() -> Unit,
): Transition<AutoEvent> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val transitionId = Any()
    val builder = AutoUnitGuardedTransitionOnBuilder(name, asState(), transitionId)
    builder.block()

    installAutoTrigger(builder.delay) { AutoEventImpl(transitionId) }
    return addTransition(builder.build())
}

/**
 * Scoped variant with full conditional control over the resulting [TransitionDirection]. Set
 * [delay] and `direction` inside the builder block.
 */
fun TransitionStateApi.autoTransitionConditionally(
    name: String? = null,
    block: AutoConditionalTransitionBuilder.() -> Unit,
): Transition<AutoEvent> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val transitionId = Any()
    val builder = AutoConditionalTransitionBuilder(name, asState(), transitionId)
    builder.block()

    installAutoTrigger(builder.delay) { AutoEventImpl(transitionId) }
    return addTransition(builder.build())
}

/** Scoped data transition with [delay], [dataProducer], `targetState`, and optional `guard`. */
fun <D : Any> TransitionStateApi.autoDataTransition(
    name: String? = null,
    block: AutoDataGuardedTransitionBuilder<D>.() -> Unit,
): Transition<AutoDataEvent<D>> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val transitionId = Any()
    val builder = AutoDataGuardedTransitionBuilder<D>(name, asState(), transitionId)
    builder.block()

    installAutoTrigger(builder.delay) {
        AutoDataEventImpl(transitionId, builder.dataProducer())
    }
    return addTransition(builder.build())
}

/** Scoped data transition with lazy [targetState] lambda, [delay], and [dataProducer]. */
fun <D : Any> TransitionStateApi.autoDataTransitionOn(
    name: String? = null,
    block: AutoDataGuardedTransitionOnBuilder<D>.() -> Unit,
): Transition<AutoDataEvent<D>> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val transitionId = Any()
    val builder = AutoDataGuardedTransitionOnBuilder<D>(name, asState(), transitionId)
    builder.block()

    installAutoTrigger(builder.delay) {
        AutoDataEventImpl(transitionId, builder.dataProducer())
    }
    return addTransition(builder.build())
}

/**
 * Installs the `onEntry`/`onExit` listener that fires [eventFactory] for the given [transitionId].
 * When [delay] is null the event fires immediately (eventless / "always"). When [delay] is
 * non-null the event fires after the delay via [CoroutineAbstraction.scheduleAfterDelay],
 * which throws if the machine was not created with coroutines support.
 */
private fun TransitionStateApi.installAutoTrigger(
    delay: Duration?,
    eventFactory: suspend () -> AutoEvent,
) {
    val sourceState = asState()
    if (delay == null) {
        sourceState.addListener(object : IState.Listener {
            override suspend fun onEntry(transitionParams: TransitionParams<*>) {
                sourceState.machine.processEvent(eventFactory())
            }
        })
    } else {
        val machine = sourceState.machine
        var cancellable: Cancellable? = null
        fun cancelAndClear() {
            cancellable?.cancel()
            cancellable = null
        }
        machine.addListener(object : StateMachine.Listener {
            override suspend fun onStopped() = cancelAndClear()
            override suspend fun onDestroyed() = cancelAndClear()
        })
        sourceState.addListener(object : IState.Listener {
            override suspend fun onEntry(transitionParams: TransitionParams<*>) {
                check(cancellable == null) { "Timer is already running - this is logical error, please report a bug" }
                cancellable = machine.coroutineAbstraction.scheduleAfterDelay(delay) {
                    machine.processEvent(eventFactory())
                }
            }

            override suspend fun onExit(transitionParams: TransitionParams<*>) = cancelAndClear()
        })
    }
}
