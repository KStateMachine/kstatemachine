package ru.nsk.kstatemachine.metainfo

import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.transition.Transition

/**
 * Additional static (designed to be immutable) info for library primitives like [IState] [Transition] etc.
 * Users may extend this interface to add their own [MetaInfo] implementations.
 * Users may combine multiple [MetaInfo] interfaces into one object.
 */
interface MetaInfo

/**
 * Standard [MetaInfo], to control export PlantUML and Mermaid feature visualization.
 */
interface IUmlMetaInfo : MetaInfo {
    /**
     * Will be mapped to "long name" for [IState], and a "label" for [Transition]
     */
    val umlLabel: String?
    /**
     * Add description lines for [IState]
     * Does not have effect for [Transition]
     */
    val umlStateDescriptions: List<String>

    /**
     * For [IState] translated to "note right of".
     * For [Transition] translated to "note on link" (supports only one note).
     * Mermaid does not support this, so it will not take any effect.
     */
    val umlNotes: List<String>
}

/**
 * [IUmlMetaInfo] Implementation is separated from its interface as a user may combine multiple [MetaInfo]
 * interfaces into one object.
 */
data class UmlMetaInfo(
    override val umlLabel: String? = null,
    override val umlStateDescriptions: List<String> = emptyList(),
    override val umlNotes: List<String> = emptyList(),
): IUmlMetaInfo