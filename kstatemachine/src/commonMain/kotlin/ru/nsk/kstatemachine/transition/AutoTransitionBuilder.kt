/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2026.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.transition

import ru.nsk.kstatemachine.event.AutoDataEvent
import ru.nsk.kstatemachine.event.autoDataEventMatcher
import ru.nsk.kstatemachine.state.IState

/** Extends [DataGuardedTransitionBuilder] DSL adding [dataProducer] field */
open class AutoDataGuardedTransitionBuilder<D : Any>(name: String?, sourceState: IState, private val transitionId: Any) :
    DataGuardedTransitionBuilder<AutoDataEvent<D>, D>(name, sourceState) {
    /** User should initialize this filed */
    lateinit var dataProducer: suspend () -> D

    @PublishedApi
    override fun build(): Transition<AutoDataEvent<D>> {
        require(this::dataProducer.isInitialized) { "dataProducer should be initialized in this transition builder" }
        eventMatcher = autoDataEventMatcher(transitionId)
        return super.build()
    }
}

/** Extends [DataGuardedTransitionOnBuilder] DSL adding [dataProducer] field */
open class AutoDataGuardedTransitionOnBuilder<D : Any>(name: String?, sourceState: IState, private val transitionId: Any) :
    DataGuardedTransitionOnBuilder<AutoDataEvent<D>, D>(name, sourceState) {
    /** User should initialize this filed */
    lateinit var dataProducer: suspend () -> D

    @PublishedApi
    override fun build(): Transition<AutoDataEvent<D>> {
        require(this::dataProducer.isInitialized) { "dataProducer should be initialized in this transition builder" }
        eventMatcher = autoDataEventMatcher(transitionId)
        return super.build()
    }
}
