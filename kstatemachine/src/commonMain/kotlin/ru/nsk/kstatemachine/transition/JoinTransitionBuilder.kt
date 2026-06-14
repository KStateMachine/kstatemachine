/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2026.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.transition

import ru.nsk.kstatemachine.event.JoinCompleteDataEvent
import ru.nsk.kstatemachine.event.JoinCompleteEvent
import ru.nsk.kstatemachine.event.joinDataEventMatcher
import ru.nsk.kstatemachine.event.joinEventMatcher
import ru.nsk.kstatemachine.metainfo.JoinTransitionMetaInfo
import ru.nsk.kstatemachine.metainfo.plus
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.state.requireMinimumJoinStates

class JoinConditionalTransitionBuilder(name: String?, sourceState: IState, val transitionId: Any) :
    ConditionalTransitionBuilder<JoinCompleteEvent>(name, sourceState) {
    /** User should initialize this filed */
    lateinit var joinStates: Set<IState>

    @PublishedApi
    override fun build(): Transition<JoinCompleteEvent> {
        require(this::joinStates.isInitialized) { "joinStates should be initialized in this transition builder" }
        requireMinimumJoinStates(joinStates)
        metaInfo += JoinTransitionMetaInfo(joinStates)
        eventMatcher = joinEventMatcher(transitionId)
        return super.build()
    }
}

class JoinUnitGuardedTransitionBuilder(name: String?, sourceState: IState, val transitionId: Any) :
    UnitGuardedTransitionBuilder<JoinCompleteEvent>(name, sourceState) {
    /** User should initialize this filed */
    lateinit var joinStates: Set<IState>

    @PublishedApi
    override fun build(): Transition<JoinCompleteEvent> {
        require(this::joinStates.isInitialized) { "joinStates should be initialized in this transition builder" }
        requireMinimumJoinStates(joinStates)
        metaInfo += JoinTransitionMetaInfo(joinStates)
        eventMatcher = joinEventMatcher(transitionId)
        return super.build()
    }
}

class JoinUnitGuardedTransitionOnBuilder(name: String?, sourceState: IState, val transitionId: Any) :
    UnitGuardedTransitionOnBuilder<JoinCompleteEvent>(name, sourceState) {
    /** User should initialize this filed */
    lateinit var joinStates: Set<IState>

    @PublishedApi
    override fun build(): Transition<JoinCompleteEvent> {
        require(this::joinStates.isInitialized) { "joinStates should be initialized in this transition builder" }
        requireMinimumJoinStates(joinStates)
        metaInfo += JoinTransitionMetaInfo(joinStates)
        eventMatcher = joinEventMatcher(transitionId)
        return super.build()
    }
}

/** Extends [DataGuardedTransitionBuilder] DSL adding [dataProducer] field */
class JoinDataGuardedTransitionBuilder<D : Any>(name: String?, sourceState: IState, val transitionId: Any) :
    DataGuardedTransitionBuilder<JoinCompleteDataEvent<D>, D>(name, sourceState) {
    /** User should initialize this filed */
    lateinit var joinStates: Set<IState>

    /** User should initialize this filed */
    lateinit var dataProducer: suspend () -> D

    @PublishedApi
    override fun build(): Transition<JoinCompleteDataEvent<D>> {
        require(this::joinStates.isInitialized) { "joinStates should be initialized in this transition builder" }
        requireMinimumJoinStates(joinStates)
        require(this::dataProducer.isInitialized) { "dataProducer should be initialized in this transition builder" }
        eventMatcher = joinDataEventMatcher(transitionId)
        return super.build()
    }
}

/** Extends [DataGuardedTransitionOnBuilder] DSL adding [dataProducer] field */
class JoinDataGuardedTransitionOnBuilder<D : Any>(name: String?, sourceState: IState, val transitionId: Any) :
    DataGuardedTransitionOnBuilder<JoinCompleteDataEvent<D>, D>(name, sourceState) {
    /** User should initialize this filed */
    lateinit var joinStates: Set<IState>

    /** User should initialize this filed */
    lateinit var dataProducer: suspend () -> D

    @PublishedApi
    override fun build(): Transition<JoinCompleteDataEvent<D>> {
        require(this::joinStates.isInitialized) { "joinStates should be initialized in this transition builder" }
        requireMinimumJoinStates(joinStates)
        require(this::dataProducer.isInitialized) { "dataProducer should be initialized in this transition builder" }
        eventMatcher = joinDataEventMatcher(transitionId)
        return super.build()
    }
}
