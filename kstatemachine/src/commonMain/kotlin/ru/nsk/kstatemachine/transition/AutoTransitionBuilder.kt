/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2026.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.transition

import ru.nsk.kstatemachine.event.AutoDataEvent
import ru.nsk.kstatemachine.event.AutoEvent
import ru.nsk.kstatemachine.event.autoDataEventMatcher
import ru.nsk.kstatemachine.event.autoEventMatcher
import ru.nsk.kstatemachine.metainfo.DelayedAutoTransitionMetaInfo
import ru.nsk.kstatemachine.metainfo.plus
import ru.nsk.kstatemachine.state.IState
import kotlin.time.Duration

/**
 * Scoped builder for [autoTransition]. Extends [UnitGuardedTransitionBuilder]<[AutoEvent]> with an
 * optional [delay] field. When [delay] is `null` (default) the transition fires immediately on
 * source-state entry; when non-null it requires `kstatemachine-coroutines` and fires after the
 * delay elapses.
 */
class AutoUnitGuardedTransitionBuilder(
    name: String?,
    sourceState: IState,
    private val transitionId: Any,
) : UnitGuardedTransitionBuilder<AutoEvent>(name, sourceState) {
    /** Time to wait before firing. `null` = immediate (no coroutines required). */
    var delay: Duration? = null

    public override fun build(): Transition<AutoEvent> {
        eventMatcher = autoEventMatcher(transitionId)
        metaInfo += delayedMetaInfoOrNull(delay)
        return super.build()
    }
}

/**
 * Scoped builder for [autoTransitionOn]. Same as [AutoUnitGuardedTransitionBuilder] but inherits
 * the lazy [targetState] lambda from [UnitGuardedTransitionOnBuilder].
 */
class AutoUnitGuardedTransitionOnBuilder(
    name: String?,
    sourceState: IState,
    private val transitionId: Any,
) : UnitGuardedTransitionOnBuilder<AutoEvent>(name, sourceState) {
    /** Time to wait before firing. `null` = immediate (no coroutines required). */
    var delay: Duration? = null

    public override fun build(): Transition<AutoEvent> {
        eventMatcher = autoEventMatcher(transitionId)
        metaInfo += delayedMetaInfoOrNull(delay)
        return super.build()
    }
}

/**
 * Scoped builder for [autoTransitionConditionally]. Same as [AutoUnitGuardedTransitionBuilder] but
 * inherits the full [direction] lambda from [ConditionalTransitionBuilder].
 */
class AutoConditionalTransitionBuilder(
    name: String?,
    sourceState: IState,
    private val transitionId: Any,
) : ConditionalTransitionBuilder<AutoEvent>(name, sourceState) {
    /** Time to wait before firing. `null` = immediate (no coroutines required). */
    var delay: Duration? = null

    public override fun build(): Transition<AutoEvent> {
        eventMatcher = autoEventMatcher(transitionId)
        metaInfo += delayedMetaInfoOrNull(delay)
        return super.build()
    }
}

/** Scoped builder for [autoDataTransition]. Extends [DataGuardedTransitionBuilder] with [dataProducer] and [delay]. */
open class AutoDataGuardedTransitionBuilder<D : Any>(
    name: String?,
    sourceState: IState,
    private val transitionId: Any,
) : DataGuardedTransitionBuilder<AutoDataEvent<D>, D>(name, sourceState) {
    /** User should initialize this field. */
    lateinit var dataProducer: suspend () -> D
    /** Time to wait before firing. `null` = immediate (no coroutines required). */
    var delay: Duration? = null

    public override fun build(): Transition<AutoDataEvent<D>> {
        require(this::dataProducer.isInitialized) { "dataProducer should be initialized in this transition builder" }
        eventMatcher = autoDataEventMatcher(transitionId)
        metaInfo += delayedMetaInfoOrNull(delay)
        return super.build()
    }
}

/** Scoped builder for [autoDataTransitionOn]. Extends [DataGuardedTransitionOnBuilder] with [dataProducer] and [delay]. */
open class AutoDataGuardedTransitionOnBuilder<D : Any>(
    name: String?,
    sourceState: IState,
    private val transitionId: Any,
) : DataGuardedTransitionOnBuilder<AutoDataEvent<D>, D>(name, sourceState) {
    /** User should initialize this field. */
    lateinit var dataProducer: suspend () -> D
    /** Time to wait before firing. `null` = immediate (no coroutines required). */
    var delay: Duration? = null

    public override fun build(): Transition<AutoDataEvent<D>> {
        require(this::dataProducer.isInitialized) { "dataProducer should be initialized in this transition builder" }
        eventMatcher = autoDataEventMatcher(transitionId)
        metaInfo += delayedMetaInfoOrNull(delay)
        return super.build()
    }
}

internal fun delayedMetaInfoOrNull(delay: Duration?): DelayedAutoTransitionMetaInfo? =
    delay?.let { DelayedAutoTransitionMetaInfo(it) }
