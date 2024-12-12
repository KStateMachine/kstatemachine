/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.metainfo

import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.transition.Transition

/**
 * Standard [MetaInfo], to control export PlantUML and Mermaid feature visualization.
 */
interface UmlMetaInfo : MetaInfo {
    /**
     * Will be mapped to "long name" for [IState], and a "label" for [Transition]
     * Default: null
     */
    val umlLabel: String?

    /**
     * Add description lines for [IState]
     * Does not have effect for [Transition]
     * Default: emptyList()
     */
    val umlStateDescriptions: List<String>

    /**
     * For [IState] translated to "note right of".
     * For [Transition] translated to "note on link" (supports only one note).
     * Mermaid does not support this, so it will not take any effect.
     * Default: emptyList()
     */
    val umlNotes: List<String>
}

/**
 * [UmlMetaInfo] Implementation is separated from its interface as a user may combine multiple [MetaInfo]
 * interfaces into one object. Data class should not be exposed to public APIs due to binary compatibility, users should
 * use [buildUmlMetaInfo] instead.
 */
interface UmlMetaInfoBuilder : UmlMetaInfo {
    override var umlLabel: String?
    override var umlStateDescriptions: List<String>
    override var umlNotes: List<String>
}

private data class UmlMetaInfoBuilderImpl(
    override var umlLabel: String? = null,
    override var umlStateDescriptions: List<String> = emptyList(),
    override var umlNotes: List<String> = emptyList(),
) : UmlMetaInfoBuilder

fun buildUmlMetaInfo(builder: UmlMetaInfoBuilder.() -> Unit): UmlMetaInfo =
    UmlMetaInfoBuilderImpl().apply(builder).copy()