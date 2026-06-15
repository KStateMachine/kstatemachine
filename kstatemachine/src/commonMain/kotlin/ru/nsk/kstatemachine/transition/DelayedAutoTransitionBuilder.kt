/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2026.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.transition

import ru.nsk.kstatemachine.event.AutoDataEvent
import ru.nsk.kstatemachine.event.AutoEvent
import ru.nsk.kstatemachine.event.autoEventMatcher
import ru.nsk.kstatemachine.metainfo.DelayedAutoTransitionMetaInfo
import ru.nsk.kstatemachine.metainfo.plus
import ru.nsk.kstatemachine.state.IState
import kotlin.time.Duration

/**
 * Common DSL surface for `delayedAuto*` builders — exposes [delay] as a settable field so it can be
 * configured inside the scoped builder lambda instead of being passed as a function argument.
 *
 * The library reads [delay] after the lambda runs to wire up the timer.
 */
interface DelayedAutoTransitionBuilderDsl {
    /**
     * User should initialize this filed.
     * Time the source state must remain active before the transition fires.
     * It is not possible to implemented as lateinit property as for other transition builder as [Duration] is an inline class.
     */
    var delay: Duration?
}

/**
 * Reads the user-set [delay], attaches a [DelayedAutoTransitionMetaInfo] so the exporter can
 * render `after Xms`, and returns the validated duration for the caller to install the timer.
 */
private fun TransitionBuilder<*>.applyDelayConfig(delay: Duration?): Duration {
    val resolved = requireNotNull(delay) { "delay should be set in this transition builder" }
    metaInfo += DelayedAutoTransitionMetaInfo(resolved)
    return resolved
}

/** Delayed analog of [UnitGuardedTransitionBuilder]<[AutoEvent]>. */
class DelayedAutoUnitGuardedTransitionBuilder(
    name: String?,
    sourceState: IState,
    private val transitionId: Any,
) : UnitGuardedTransitionBuilder<AutoEvent>(name, sourceState), DelayedAutoTransitionBuilderDsl {
    override var delay: Duration? = null

    /** For internal library use only. It is public only as used from other kstatemachine-coroutines artifact */
    public override fun build(): Transition<AutoEvent> {
        applyDelayConfig(delay)
        eventMatcher = autoEventMatcher(transitionId)
        return super.build()
    }
}

/** Delayed analog of [UnitGuardedTransitionOnBuilder]<[AutoEvent]>. */
class DelayedAutoUnitGuardedTransitionOnBuilder(
    name: String?,
    sourceState: IState,
    private val transitionId: Any,
) : UnitGuardedTransitionOnBuilder<AutoEvent>(name, sourceState), DelayedAutoTransitionBuilderDsl {
    override var delay: Duration? = null

    /** For internal library use only. It is public only as used from other kstatemachine-coroutines artifact */
    public override fun build(): Transition<AutoEvent> {
        applyDelayConfig(delay)
        eventMatcher = autoEventMatcher(transitionId)
        return super.build()
    }
}

/** Delayed analog of [ConditionalTransitionBuilder]<[AutoEvent]>. */
class DelayedAutoConditionalTransitionBuilder(
    name: String?,
    sourceState: IState,
    private val transitionId: Any,
) : ConditionalTransitionBuilder<AutoEvent>(name, sourceState), DelayedAutoTransitionBuilderDsl {
    override var delay: Duration? = null

    /** For internal library use only. It is public only as used from other kstatemachine-coroutines artifact */
    public override fun build(): Transition<AutoEvent> {
        applyDelayConfig(delay)
        eventMatcher = autoEventMatcher(transitionId)
        return super.build()
    }
}

/**
 * Delayed analog of [AutoDataGuardedTransitionBuilder]. Inherits [AutoDataGuardedTransitionBuilder.dataProducer]
 * (set inside the lambda) and the `autoDataEventMatcher` wiring; adds a required [delay] field.
 */
class DelayedAutoDataGuardedTransitionBuilder<D : Any>(
    name: String?,
    sourceState: IState,
    transitionId: Any,
) : AutoDataGuardedTransitionBuilder<D>(name, sourceState, transitionId), DelayedAutoTransitionBuilderDsl {
    override var delay: Duration? = null

    /** For internal library use only. It is public only as used from other kstatemachine-coroutines artifact */
    public override fun build(): Transition<AutoDataEvent<D>> {
        applyDelayConfig(delay)
        return super.build()
    }
}

/** Delayed analog of [AutoDataGuardedTransitionOnBuilder]. */
class DelayedAutoDataGuardedTransitionOnBuilder<D : Any>(
    name: String?,
    sourceState: IState,
    transitionId: Any,
) : AutoDataGuardedTransitionOnBuilder<D>(name, sourceState, transitionId), DelayedAutoTransitionBuilderDsl {
    override var delay: Duration? = null

    /** For internal library use only. It is public only as used from other kstatemachine-coroutines artifact */
    public override fun build(): Transition<AutoDataEvent<D>> {
        applyDelayConfig(delay)
        return super.build()
    }
}
