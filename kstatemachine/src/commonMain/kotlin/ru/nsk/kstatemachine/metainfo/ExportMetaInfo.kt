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
 * If the hint takes an effect (applied correctly), the conditional lambda will not be called even if
 * [unsafeCallConditionalLambdas] is true.
 * You can specify multiple [StateResolutionHint] instances for the same construction to cover all internal branches.
 * User is responsible to provide correct hints.
 */
class StateResolutionHint(
    val description: String,
    /** Allows to specify parallel target states */
    val targetStates: Set<IState>,
) : ResolutionHint {
    constructor(
        description: String,
        targetState: IState,
    ) : this(description, setOf(targetState))

    init {
        require(targetStates.isNotEmpty()) {
            "targetStates must be non-empty, use single state or multiple states for parallel transitions"
        }
    }
}

/**
 * To be used with state/transition constructions where some conditional lambda uses [EventAndArgument] instance.
 * If the hint takes an effect (applied correctly) and [unsafeCallConditionalLambdas] flag is true the conditional lambda
 * will be called with specified [EventAndArgument] instead of default fake ([ExportPlantUmlEvent]).
 * You can specify multiple [EventAndArgumentResolutionHint] instances for the same construction to cover all internal branches.
 * User is responsible to provide correct hints.
 */
class EventAndArgumentResolutionHint(
    val description: String,
    val event: Event,
    val argument: Any? = null
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
    override var resolutionHints: Set<ResolutionHint>
}

private data class ExportMetaInfoBuilderImpl(
    override var resolutionHints: Set<ResolutionHint> = emptySet(),
) : ExportMetaInfoBuilder

fun buildExportMetaInfo(builder: ExportMetaInfoBuilder.() -> Unit): ExportMetaInfo =
    ExportMetaInfoBuilderImpl().apply(builder).copy()