/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import ru.nsk.kstatemachine.event.DataEvent
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.event.EventMatcher
import ru.nsk.kstatemachine.metainfo.MetaInfo
import ru.nsk.kstatemachine.transition.DefaultTransition
import ru.nsk.kstatemachine.transition.TransitionParams
import ru.nsk.kstatemachine.transition.TransitionType

/** Internal event fired when all join-point states become simultaneously active. */
internal data class JoinCompleteEvent(val joinId: Any) : Event

/** Internal data-carrying variant of [JoinCompleteEvent], used by [joinDataTransition]. */
internal data class DataJoinCompleteEvent<D : Any>(val joinId: Any, override val data: D) : DataEvent<D>

/** Attached to the join transition so the exporter can render `<<join>>` notation. */
internal data class JoinTransitionMetaInfo(
    val joinPoints: Set<IState>,
    val joinName: String,
) : MetaInfo

private fun joinEventMatcher(joinId: Any): EventMatcher<JoinCompleteEvent> =
    object : EventMatcher<JoinCompleteEvent> {
        override val eventClass = JoinCompleteEvent::class
        override suspend fun match(value: Event) =
            value is JoinCompleteEvent && value.joinId === joinId
    }

private fun dataJoinEventMatcher(joinId: Any): EventMatcher<DataJoinCompleteEvent<*>> =
    object : EventMatcher<DataJoinCompleteEvent<*>> {
        @Suppress("UNCHECKED_CAST")
        override val eventClass = DataJoinCompleteEvent::class as kotlin.reflect.KClass<DataJoinCompleteEvent<*>>
        override suspend fun match(value: Event) =
            value is DataJoinCompleteEvent<*> && value.joinId === joinId
    }

/**
 * Adds a **UML join** transition to this [ChildMode.PARALLEL] state.
 *
 * Each element of [joinPoints] is a state inside one parallel region that acts as
 * that region's synchronization point. When **all** join-point states become active
 * simultaneously the outgoing transition fires automatically to [targetState].
 *
 * **Soft blocking**: join-point states should carry no outgoing user transitions.
 * The parallel state's event-routing algorithm finds no matching transition in a
 * join-point state and falls back to the parallel parent's own transitions, which
 * only contain the internal [JoinCompleteEvent] transition. All other events are
 * silently ignored for that region — by convention, not enforcement.
 *
 * @param joinPoints at least 2 states, one per parallel region.
 * @param name optional name for the transition (also used as the `<<join>>` node name in exports).
 * @param targetState the non-data state to enter after all regions have joined.
 *   Use [joinDataTransition] to target a [DataState].
 */
fun IState.joinTransition(
    vararg joinPoints: IState,
    name: String? = null,
    targetState: State,
) {
    require(joinPoints.size >= 2) {
        "joinTransition requires at least 2 join points, got ${joinPoints.size}"
    }
    check(childMode == ChildMode.PARALLEL) {
        "joinTransition must be called on a state with ChildMode.PARALLEL, was $childMode on $this"
    }

    val joinId = Any()
    val joinPointSet = joinPoints.toSet()
    val parallelState = this
    val joinName = (name ?: "join_${joinId.hashCode().toString(16)}").replace(Regex("[ -]"), "_")

    // IState.Listener.onEntry is suspend, so processEvent() is called directly within the same
    // coroutine context. If another event is currently processing, QueuePendingEventHandler queues
    // JoinCompleteEvent and drains it after the current event finishes.
    for (joinPoint in joinPoints) {
        joinPoint.addListener(object : IState.Listener {
            override suspend fun onEntry(transitionParams: TransitionParams<*>) {
                if (joinPointSet.all { it.isActive }) {
                    parallelState.machine.processEvent(JoinCompleteEvent(joinId))
                }
            }
        })
    }

    addTransition(
        DefaultTransition(
            name = name,
            eventMatcher = joinEventMatcher(joinId),
            type = TransitionType.LOCAL,
            metaInfo = JoinTransitionMetaInfo(joinPointSet, joinName),
            sourceState = this,
            targetState = targetState,
        )
    )
}

/**
 * Type-safe variant of [joinTransition] that targets a [DataState].
 *
 * [dataProducer] is called once, synchronously, at the moment all join-point states become
 * simultaneously active. Its return value is fed to [targetState] as its entry data, replacing
 * the need for a matching [DataEvent] on the triggering event.
 *
 * @param joinPoints at least 2 states, one per parallel region.
 * @param name optional name for the transition (also used as the `<<join>>` node name in exports).
 * @param targetState the [DataState] to enter after all regions have joined.
 * @param dataProducer lambda invoked at join time to produce the data for [targetState].
 */
fun <D : Any> IState.joinDataTransition(
    vararg joinPoints: IState,
    name: String? = null,
    targetState: DataState<D>,
    dataProducer: suspend () -> D,
) {
    require(joinPoints.size >= 2) {
        "joinDataTransition requires at least 2 join points, got ${joinPoints.size}"
    }
    check(childMode == ChildMode.PARALLEL) {
        "joinDataTransition must be called on a state with ChildMode.PARALLEL, was $childMode on $this"
    }

    val joinId = Any()
    val joinPointSet = joinPoints.toSet()
    val parallelState = this
    val joinName = (name ?: "join_${joinId.hashCode().toString(16)}").replace(Regex("[ -]"), "_")

    // DataJoinCompleteEvent<D> is a DataEvent<D>, so DefaultDataState.onDoEnter picks up the data
    // through its existing DataEvent branch — no custom DataExtractor needed.
    for (joinPoint in joinPoints) {
        joinPoint.addListener(object : IState.Listener {
            override suspend fun onEntry(transitionParams: TransitionParams<*>) {
                if (joinPointSet.all { it.isActive }) {
                    parallelState.machine.processEvent(DataJoinCompleteEvent(joinId, dataProducer()))
                }
            }
        })
    }

    addTransition(
        DefaultTransition(
            name = name,
            eventMatcher = dataJoinEventMatcher(joinId),
            type = TransitionType.LOCAL,
            metaInfo = JoinTransitionMetaInfo(joinPointSet, joinName),
            sourceState = this,
            targetState = targetState,
        )
    )
}
