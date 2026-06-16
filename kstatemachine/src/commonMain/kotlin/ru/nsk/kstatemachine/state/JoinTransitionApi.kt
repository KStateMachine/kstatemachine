/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2026.
 * All rights reserved.
 */

@file:OptIn(ExperimentalContracts::class)

package ru.nsk.kstatemachine.state

import ru.nsk.kstatemachine.event.JoinCompleteDataEvent
import ru.nsk.kstatemachine.event.JoinCompleteDataEventImpl
import ru.nsk.kstatemachine.event.JoinCompleteEvent
import ru.nsk.kstatemachine.event.JoinCompleteEventImpl
import ru.nsk.kstatemachine.event.joinEventMatcher
import ru.nsk.kstatemachine.metainfo.JoinTransitionMetaInfo
import ru.nsk.kstatemachine.metainfo.MetaInfo
import ru.nsk.kstatemachine.metainfo.plus
import ru.nsk.kstatemachine.transition.DefaultTransition
import ru.nsk.kstatemachine.transition.JoinConditionalTransitionBuilder
import ru.nsk.kstatemachine.transition.JoinDataGuardedTransitionBuilder
import ru.nsk.kstatemachine.transition.JoinDataGuardedTransitionOnBuilder
import ru.nsk.kstatemachine.transition.JoinUnitGuardedTransitionBuilder
import ru.nsk.kstatemachine.transition.JoinUnitGuardedTransitionOnBuilder
import ru.nsk.kstatemachine.transition.Transition
import ru.nsk.kstatemachine.transition.TransitionType
import ru.nsk.kstatemachine.transition.TransitionType.LOCAL
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * vararg [joinTransition] overload, protects from compilation of less than two join points.
 */
suspend fun TransitionStateApi.joinTransition(
    joinState1: IState,
    joinState2: IState,
    vararg joinStates: IState,
    name: String? = null,
    targetState: State,
    type: TransitionType = LOCAL,
    metaInfo: MetaInfo? = null,
): Transition<JoinCompleteEvent> =
    joinTransition(name, setOf(joinState1, joinState2, *joinStates), targetState, type, metaInfo)

/**
 * Adds a **UML join** transition to this [ChildMode.PARALLEL] state.
 *
 * Each element of [joinStates] is a state inside one parallel region that acts as
 * that region's synchronization point. When **all** join-point states become active
 * simultaneously the outgoing transition fires automatically to [targetState].
 *
 * **Soft blocking**: join-point states should carry no outgoing user transitions.
 * The parallel state's event-routing algorithm finds no matching transition in a
 * join-point state and falls back to the parallel parent's own transitions, which
 * only contain the internal [JoinCompleteEvent] transition. All other events are
 * silently ignored for that region — by convention, not enforcement.
 *
 * @param joinStates at least 2 states, one per parallel region.
 * @param name optional name for the transition (also used as the `<<join>>` node name in exports).
 * @param targetState the non-data state to enter after all regions have joined.
 *   Use [joinDataTransition] to target a [DataState].
 */
suspend fun TransitionStateApi.joinTransition(
    name: String? = null,
    joinStates: Set<IState>,
    targetState: State,
    type: TransitionType = LOCAL,
    metaInfo: MetaInfo? = null,
): Transition<JoinCompleteEvent> {
    requireJoinPrerequisites(joinStates)
    val transitionId = Any()
    installJoinCompleteEventTrigger(transitionId, joinStates)
    return addTransition(
        DefaultTransition(
            name = name,
            eventMatcher = joinEventMatcher(transitionId),
            type = type,
            metaInfo = metaInfo + JoinTransitionMetaInfo(joinStates),
            sourceState = asState(),
            targetState = targetState,
        )
    )
}

suspend fun TransitionStateApi.joinTransition(
    name: String? = null,
    block: JoinUnitGuardedTransitionBuilder.() -> Unit
): Transition<JoinCompleteEvent> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    requireStateIsParallel()
    val transitionId = Any()
    val builder = JoinUnitGuardedTransitionBuilder(name, asState(), transitionId)
    builder.block()

    installJoinCompleteEventTrigger(transitionId, builder.joinStates)
    return addTransition(builder.build())
}

suspend fun TransitionStateApi.joinTransitionOn(
    name: String? = null,
    block: JoinUnitGuardedTransitionOnBuilder.() -> Unit,
): Transition<JoinCompleteEvent> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    requireStateIsParallel()
    val transitionId = Any()
    val builder = JoinUnitGuardedTransitionOnBuilder(name, asState(), transitionId)
    builder.block()

    installJoinCompleteEventTrigger(transitionId, builder.joinStates)
    return addTransition(builder.build())
}

suspend fun TransitionStateApi.joinTransitionConditionally(
    name: String? = null,
    block: JoinConditionalTransitionBuilder.() -> Unit
): Transition<JoinCompleteEvent> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val transitionId = Any()
    val builder = JoinConditionalTransitionBuilder(name, asState(), transitionId)
    builder.block()

    installJoinCompleteEventTrigger(transitionId, builder.joinStates)
    return addTransition(builder.build())
}

suspend fun <D : Any> TransitionStateApi.joinDataTransition(
    name: String? = null,
    block: JoinDataGuardedTransitionBuilder<D>.() -> Unit
): Transition<JoinCompleteDataEvent<D>> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    requireStateIsParallel()
    val transitionId = Any()
    val builder = JoinDataGuardedTransitionBuilder<D>(name, asState(), transitionId)
    builder.block()

    installJoinCompleteDataEventTrigger(transitionId, builder.joinStates, builder.dataProducer)
    return addTransition(builder.build())
}

suspend fun <D : Any> TransitionStateApi.joinDataTransitionOn(
    name: String? = null,
    block: JoinDataGuardedTransitionOnBuilder<D>.() -> Unit
): Transition<JoinCompleteDataEvent<D>> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    requireStateIsParallel()
    val transitionId = Any()
    val builder = JoinDataGuardedTransitionOnBuilder<D>(name, asState(), transitionId)
    builder.block()

    installJoinCompleteDataEventTrigger(transitionId, builder.joinStates, builder.dataProducer)
    return addTransition(builder.build())
}


private fun TransitionStateApi.requireStateIsParallel() {
    val thisState = asState()
    require(thisState.childMode == ChildMode.PARALLEL) {
        "joinTransition must be called on a state with ${ChildMode.PARALLEL}, was ${thisState.childMode} on $thisState"
    }
}

private fun TransitionStateApi.requireJoinPrerequisites(joinStates: Collection<IState>) {
    requireStateIsParallel()
    requireMinimumJoinStates(joinStates)
}

internal fun requireMinimumJoinStates(joinStates: Collection<IState>) {
    require(joinStates.size >= 2) {
        "joinTransition requires at least 2 unique join states (points), got ${joinStates.size}"
    }
}

/**
 * Installs the `onEntry` listener that fires the [JoinCompleteEventImpl] for the non-data variants.
 */
private suspend fun TransitionStateApi.installJoinCompleteEventTrigger(transitionId: Any, joinStates: Set<IState>) {
    onActiveAllOf(joinStates) {
        asState().machine.processEvent(JoinCompleteEventImpl(transitionId))
    }
}

/**
 * Installs the `onEntry` listener that fires the [JoinCompleteDataEventImpl].
 */
private suspend fun <D : Any> TransitionStateApi.installJoinCompleteDataEventTrigger(
    transitionId: Any,
    joinStates: Set<IState>,
    dataProducer: suspend () -> D
) {
    onActiveAllOf(joinStates) {
        asState().machine.processEvent(JoinCompleteDataEventImpl(transitionId, dataProducer()))
    }
}