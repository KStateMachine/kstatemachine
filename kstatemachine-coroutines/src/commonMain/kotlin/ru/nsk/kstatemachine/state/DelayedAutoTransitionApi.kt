/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.nsk.kstatemachine.coroutines.CoroutinesLibCoroutineAbstraction
import ru.nsk.kstatemachine.event.AutoDataEvent
import ru.nsk.kstatemachine.event.AutoEvent
import ru.nsk.kstatemachine.metainfo.DelayedAutoTransitionMetaInfo
import ru.nsk.kstatemachine.metainfo.MetaInfo
import ru.nsk.kstatemachine.metainfo.plus
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.transition.DefaultTransition
import ru.nsk.kstatemachine.transition.Transition
import ru.nsk.kstatemachine.transition.TransitionParams
import ru.nsk.kstatemachine.transition.TransitionType
import ru.nsk.kstatemachine.transition.TransitionType.LOCAL
import kotlin.time.Duration

/**
 * Adds a UML time-event ("after Xms") transition to this state.
 *
 * The timer starts when the source state is entered, fires after [delay] elapses, and is
 * automatically cancelled when the state is exited or when the machine is stopped or destroyed.
 * On re-entry the timer restarts from zero.
 *
 * [guard] is evaluated at fire time. If it returns false the state stays put — the timer does NOT
 * auto-restart; it will fire again only on the next entry. This matches UML run-to-completion.
 *
 * Requires a machine created with `createStateMachine` (the coroutines variant). Calling this on
 * a machine created with `createStdLibStateMachine` throws [IllegalArgumentException], because
 * the timer needs a coroutine scope to live in. The lifecycle (entry/exit/stop/destroy) mirrors
 * the one used by [asyncScopedAction].
 */
fun TransitionStateApi.delayedAutoTransition(
    delay: Duration,
    name: String? = null,
    targetState: State? = null,
    type: TransitionType = LOCAL,
    metaInfo: MetaInfo? = null,
): Transition<AutoEvent> {
    val transitionId = Any()
    installDelayedAutoDataEventTrigger(transitionId, delay)
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
 * Type-safe delayed transition that targets a [DataState].
 *
 * [dataProducer] is invoked once per firing, after [delay] has elapsed (not at registration time).
 * The fired event is a [DataEvent], consumed by `DefaultDataState.onDoEnter`'s data branch.
 *
 * If [guard] is supplied and returns false at fire time, no transition occurs and [dataProducer]'s
 * result is discarded (the timer does not auto-restart). [dataProducer] always runs first because
 * it is required to construct the carrier event.
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
            eventMatcher = autoDataEventMatcher(transitionId),
            type = type,
            metaInfo = metaInfo + DelayedAutoTransitionMetaInfo(delay),
            sourceState = asState(),
            targetState = targetState,
        )
    )
}

/**
 * Wires the entry/exit/stop/destroy listeners that own the timer [Job]. Same lifecycle as
 * `asyncScopedAction` in `StateCoroutines.kt:38-65`: launch on entry; cancel on exit; cancel on
 * machine stop/destroy (which do NOT trigger `onExit`).
 */
private fun <D> TransitionStateApi.installDelayedAutoDataEventTrigger(
    transitionId: Any,
    delay: Duration,
    dataProducer: suspend () -> D
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
                sourceState.machine.processEvent(AutoDataEventImpl(transitionId, dataProducer()))
            }
        }

        override suspend fun onExit(transitionParams: TransitionParams<*>) {
            cancelAndClearJob()
        }
    })
}
