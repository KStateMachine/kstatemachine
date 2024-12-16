/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.metainfo

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.state.RedirectPseudoState
import ru.nsk.kstatemachine.transition.EventAndArgument

/**
 * Hint to be used with [ExportMetaInfo]
 */
sealed interface ResolutionHint

/**
 * To be used with state/transition constructions where some conditional lambda should return [IState] type.
 * This hint does not require [unsafeCallConditionalLambdas] flag to be true.
 * You can specify multiple [StateResolutionHint] instances for the same construction to cover all internal lambda
 * branches.
 * User is responsible to provide correct hints.
 */
internal class StateResolutionHint(
    val description: String,
    /** Allows to specify parallel target states. Must be non-empty */
    val targetStates: Set<IState>,
) : ResolutionHint {
    init {
        require(targetStates.isNotEmpty()) {
            "targetStates must be non-empty, use single state or multiple states for parallel transitions"
        }
    }
}

/**
 * To be used with state/transition constructions where some conditional lambda uses [EventAndArgument] instance.
 * If the hint takes an effect (applied correctly) and [unsafeCallConditionalLambdas] flag is true the conditional lambda
 * will be called with specified [EventAndArgument] instead of default fake [ExportPlantUmlEvent].
 * You can specify multiple [EventAndArgumentResolutionHint] instances for the same construction to cover all internal
 * lambda branches.
 * User is responsible to provide correct hints.
 */
internal class EventAndArgumentResolutionHint(
    val description: String,
    val event: Event,
    val argument: Any?
) : ResolutionHint {
    val eventAndArgument = EventAndArgument(event, argument)
}

/**
 * Allows to ignore an effect of export's feature [unsafeCallConditionalLambdas] flag for certain conditional transition
 * or conditional state [RedirectPseudoState]. Conditional lambda will not be called if this meta info is applied.
 */
object IgnoreUnsafeCallConditionalLambdasMetaInfo : MetaInfo

/**
 * Standard [MetaInfo], to control unsafe export feature.
 */
interface ExportMetaInfo : MetaInfo {
    /**
     * Default: emptySet()
     */
    val resolutionHints: Set<ResolutionHint>
}

/**
 * [ExportMetaInfo] Implementation is separated from its interface as a user may combine multiple [MetaInfo]
 * interfaces into one object. Data class should not be exposed to public APIs due to binary compatibility, users should
 * use [buildExportMetaInfo] instead.
 */
interface ExportMetaInfoBuilder : ExportMetaInfo {
    /** See [StateResolutionHint] */
    fun addStateResolutionHint(description: String, targetState: IState)

    /** See [StateResolutionHint] */
    fun addStateResolutionHint(description: String, targetStates: Set<IState>)

    /** See [EventAndArgumentResolutionHint] */
    fun addEventAndArgumentResolutionHint(description: String, event: Event, argument: Any? = null)
}

private data class ExportMetaInfoBuilderImpl(
    override val resolutionHints: MutableSet<ResolutionHint> = mutableSetOf<ResolutionHint>(),
) : ExportMetaInfoBuilder {
    override fun addEventAndArgumentResolutionHint(description: String, event: Event, argument: Any?) {
        resolutionHints += EventAndArgumentResolutionHint(description, event, argument)
    }

    override fun addStateResolutionHint(description: String, targetState: IState) {
        resolutionHints += StateResolutionHint(description, setOf(targetState))
    }

    override fun addStateResolutionHint(description: String, targetStates: Set<IState>) {
        resolutionHints += StateResolutionHint(description, targetStates)
    }
}

fun buildExportMetaInfo(builder: ExportMetaInfoBuilder.() -> Unit): ExportMetaInfo =
    ExportMetaInfoBuilderImpl().apply(builder).copy()