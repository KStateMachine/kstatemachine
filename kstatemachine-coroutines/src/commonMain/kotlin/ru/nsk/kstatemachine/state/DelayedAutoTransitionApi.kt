/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2026.
 * All rights reserved.
 */

@file:OptIn(ExperimentalContracts::class)

package ru.nsk.kstatemachine.state

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.nsk.kstatemachine.coroutines.CoroutinesLibCoroutineAbstraction
import ru.nsk.kstatemachine.event.AutoDataEvent
import ru.nsk.kstatemachine.event.AutoDataEventImpl
import ru.nsk.kstatemachine.event.AutoEvent
import ru.nsk.kstatemachine.event.AutoEventImpl
import ru.nsk.kstatemachine.event.autoDataEventMatcher
import ru.nsk.kstatemachine.event.autoEventMatcher
import ru.nsk.kstatemachine.metainfo.DelayedAutoTransitionMetaInfo
import ru.nsk.kstatemachine.metainfo.MetaInfo
import ru.nsk.kstatemachine.metainfo.plus
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.transition.DefaultTransition
import ru.nsk.kstatemachine.transition.DelayedAutoConditionalTransitionBuilder
import ru.nsk.kstatemachine.transition.DelayedAutoDataGuardedTransitionBuilder
import ru.nsk.kstatemachine.transition.DelayedAutoDataGuardedTransitionOnBuilder
import ru.nsk.kstatemachine.transition.DelayedAutoUnitGuardedTransitionBuilder
import ru.nsk.kstatemachine.transition.DelayedAutoUnitGuardedTransitionOnBuilder
import ru.nsk.kstatemachine.transition.Transition
import ru.nsk.kstatemachine.transition.TransitionParams
import ru.nsk.kstatemachine.transition.TransitionType
import ru.nsk.kstatemachine.transition.TransitionType.LOCAL
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration

/**
 * Adds a UML time-event ("after Xms") transition to this state.
 *
 * The timer starts when the source state is entered, fires after [delay] elapses, and is
 * automatically cancelled when the state is exited or when the machine is stopped or destroyed.
 * On re-entry the timer restarts from zero. Reuses [AutoEvent] under the hood — a delayed
 * transition is semantically just an auto-transition whose firing is postponed by a timer.
 *
 * Requires a machine created with `createStateMachine` (the coroutines variant). Calling this on
 * a machine created with `createStdLibStateMachine` throws [IllegalArgumentException]; the lifecycle
 * (entry/exit/stop/destroy) mirrors [asyncScopedAction].
 */
fun TransitionStateApi.delayedAutoTransition(
    delay: Duration,
    name: String? = null,
    targetState: State? = null,
    type: TransitionType = LOCAL,
    metaInfo: MetaInfo? = null,
): Transition<AutoEvent> {
    val transitionId = Any()
    installDelayedAutoEventTrigger(transitionId, delay)
    return addTransition(
        DefaultTransition(
            name = name,
            eventMatcher = autoEventMatcher(transitionId),
            type = type,
            metaInfo = metaInfo + DelayedAutoTransitionMetaInfo(delay),
            sourceState = asState(),
            targetState = targetState,
        )
    )
}

/**
 * Scoped delayed transition — set [delay] (required), `guard`, and `targetState` inside the lambda.
 *
 * ```
 * delayedAutoTransition {
 *     delay = 2.seconds
 *     guard = { ready }
 *     targetState = nextState
 * }
 * ```
 *
 * `inline` so `builder.build()` (`@PublishedApi internal` in core) resolves at the user call site.
 */
fun TransitionStateApi.delayedAutoTransition(
    name: String? = null,
    block: DelayedAutoUnitGuardedTransitionBuilder.() -> Unit,
): Transition<AutoEvent> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val transitionId = Any()
    val builder = DelayedAutoUnitGuardedTransitionBuilder(name, asState(), transitionId)
    builder.block()
    val transition = builder.build()
    installDelayedAutoEventTrigger(transitionId, builder.delay!!)
    return addTransition(transition)
}

/**
 * Delayed transition with a guard and/or lazy target lookup.
 *
 * ```
 * delayedAutoTransitionOn {
 *     delay = 30.seconds
 *     guard = { !inputBlocked }
 *     targetState = { screensaver }
 * }
 * ```
 */
fun TransitionStateApi.delayedAutoTransitionOn(
    name: String? = null,
    block: DelayedAutoUnitGuardedTransitionOnBuilder.() -> Unit,
): Transition<AutoEvent> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val transitionId = Any()
    val builder = DelayedAutoUnitGuardedTransitionOnBuilder(name, asState(), transitionId)
    builder.block()
    val transition = builder.build()
    installDelayedAutoEventTrigger(transitionId, builder.delay!!)
    return addTransition(transition)
}

/** Delayed transition with full conditional control over the resulting [TransitionDirection]. */
fun TransitionStateApi.delayedAutoTransitionConditionally(
    name: String? = null,
    block: DelayedAutoConditionalTransitionBuilder.() -> Unit,
): Transition<AutoEvent> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val transitionId = Any()
    val builder = DelayedAutoConditionalTransitionBuilder(name, asState(), transitionId)
    builder.block()
    val transition = builder.build()
    installDelayedAutoEventTrigger(transitionId, builder.delay!!)
    return addTransition(transition)
}

/**
 * Type-safe delayed transition that targets a [DataState].
 *
 * [dataProducer] is invoked once per firing, after [delay] has elapsed (not at registration time).
 * The fired event is an [AutoDataEvent], consumed by `DefaultDataState.onDoEnter`'s data branch.
 */
fun <D : Any> TransitionStateApi.delayedAutoDataTransition(
    delay: Duration,
    name: String? = null,
    targetState: DataState<D>,
    type: TransitionType = LOCAL,
    metaInfo: MetaInfo? = null,
    dataProducer: suspend () -> D,
): Transition<AutoDataEvent<D>> {
    val transitionId = Any()
    installDelayedAutoDataEventTrigger(transitionId, delay, dataProducer)
    return addTransition(
        DefaultTransition(
            name = name,
            eventMatcher = autoDataEventMatcher<D>(transitionId),
            type = type,
            metaInfo = metaInfo + DelayedAutoTransitionMetaInfo(delay),
            sourceState = asState(),
            targetState = targetState,
        )
    )
}

/**
 * Self-targeted delayed data transition. Available only inside a [DataTransitionStateApi] block, so
 * the source state's data type matches the produced data — same shape as [autoDataTransition] on
 * [DataTransitionStateApi].
 */
fun <D : Any> DataTransitionStateApi<D>.delayedAutoDataTransition(
    delay: Duration,
    name: String? = null,
    type: TransitionType = LOCAL,
    metaInfo: MetaInfo? = null,
    dataProducer: suspend () -> D,
): Transition<AutoDataEvent<D>> {
    val transitionId = Any()
    installDelayedAutoDataEventTrigger(transitionId, delay, dataProducer)
    return addTransition(
        DefaultTransition(
            name = name,
            eventMatcher = autoDataEventMatcher<D>(transitionId),
            type = type,
            metaInfo = metaInfo + DelayedAutoTransitionMetaInfo(delay),
            sourceState = asState(),
            targetState = null,
        )
    )
}

/**
 * Scoped delayed data transition — set [delay], [DelayedAutoDataGuardedTransitionBuilder.dataProducer],
 * `targetState`, etc. inside the lambda.
 */
fun <D : Any> TransitionStateApi.delayedAutoDataTransition(
    name: String? = null,
    block: DelayedAutoDataGuardedTransitionBuilder<D>.() -> Unit,
): Transition<AutoDataEvent<D>> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val transitionId = Any()
    val builder = DelayedAutoDataGuardedTransitionBuilder<D>(name, asState(), transitionId)
    builder.block()
    val transition = builder.build()
    installDelayedAutoDataEventTrigger(transitionId, builder.delay!!, builder.dataProducer)
    return addTransition(transition)
}

/** Delayed data transition with a guard and/or lazy target lookup. */
fun <D : Any> TransitionStateApi.delayedAutoDataTransitionOn(
    name: String? = null,
    block: DelayedAutoDataGuardedTransitionOnBuilder<D>.() -> Unit,
): Transition<AutoDataEvent<D>> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val transitionId = Any()
    val builder = DelayedAutoDataGuardedTransitionOnBuilder<D>(name, asState(), transitionId)
    builder.block()
    val transition = builder.build()
    installDelayedAutoDataEventTrigger(transitionId, builder.delay!!, builder.dataProducer)
    return addTransition(transition)
}

private fun TransitionStateApi.installDelayedAutoEventTrigger(transitionId: Any, delay: Duration) {
    installDelayedAutoEventTriggerImpl(transitionId, delay) { AutoEventImpl(transitionId) }
}

private fun <D : Any> TransitionStateApi.installDelayedAutoDataEventTrigger(
    transitionId: Any,
    delay: Duration,
    dataProducer: suspend () -> D,
) {
    installDelayedAutoEventTriggerImpl(transitionId, delay) { AutoDataEventImpl(transitionId, dataProducer()) }
}

/**
 * Wires the entry/exit/stop/destroy listeners that own the timer [Job]. Same lifecycle as
 * `asyncScopedAction`: launch on entry; cancel on exit; cancel on machine stop/destroy
 * (those do NOT trigger `onExit`).
 */
private fun TransitionStateApi.installDelayedAutoEventTriggerImpl(
    transitionId: Any,
    delay: Duration,
    eventFactory: suspend () -> AutoEvent,
) {
    val sourceState = asState()
    val abstraction = sourceState.machine.coroutineAbstraction
    require(abstraction is CoroutinesLibCoroutineAbstraction) {
        "delayedAutoTransition requires a StateMachine created with coroutines support (createStateMachine)"
    }
    var job: Job? = null
    fun cancelAndClearJob() {
        job?.let {
            it.cancel()
            job = null
        }
    }
    sourceState.machine.addListener(object : StateMachine.Listener {
        override suspend fun onStopped() = cancelAndClearJob()
        override suspend fun onDestroyed() = cancelAndClearJob()
    })
    sourceState.addListener(object : IState.Listener {
        override suspend fun onEntry(transitionParams: TransitionParams<*>) {
            check(job == null) { "Job is already running - this is logical error, please report a bug" }
            job = abstraction.scope.launch {
                delay(delay)
                sourceState.machine.processEvent(eventFactory())
            }
        }

        override suspend fun onExit(transitionParams: TransitionParams<*>) {
            cancelAndClearJob()
        }
    })
}
