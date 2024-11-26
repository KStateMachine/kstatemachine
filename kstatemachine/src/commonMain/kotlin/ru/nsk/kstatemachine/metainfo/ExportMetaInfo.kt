/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.metainfo

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.transition.TransitionDirection

sealed interface ResolutionHint
class StateResolutionHint(targetState: IState) : ResolutionHint
class EventResolutionHint(event: Event) : ResolutionHint
class DirectionResolutionHint(direction: TransitionDirection) : ResolutionHint

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