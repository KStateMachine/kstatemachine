package ru.nsk.kstatemachine

/**
 * Additional static (designed to be immutable) info for library primitives.
 * Users may extend this interface to add their own [MetaInfo] implementations
 */
interface MetaInfo

/**
 * Standard meta info, to control export PlantUML and Mermaid feature visualization.
 */
interface IUmlMetaInfo : MetaInfo {
    /**
     * Will be mapped to "long name" for state, and a "label" for transition
     */
    val umlLabel: String?
    val stateDescriptions: List<String>
    val notes: List<String>
}

/**
 * [IUmlMetaInfo] Implementation is separated from its interface as a user may combine multiple [MetaInfo]
 * interfaces into one object.
 */
data class UmlMetaInfo(
    override val umlLabel: String? = null,
    override val stateDescriptions: List<String> = emptyList(),
    override val notes: List<String> = emptyList(),
): IUmlMetaInfo