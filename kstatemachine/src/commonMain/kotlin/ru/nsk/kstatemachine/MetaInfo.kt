package ru.nsk.kstatemachine

sealed interface MetaInfo
interface StateMetaInfo : MetaInfo
interface TransitionMetaInfo : MetaInfo
interface UmlMetaInfo: StateMetaInfo, TransitionMetaInfo {
    val umlLabel: String
}
fun umlLabel(label: String) = object : UmlMetaInfo {
    override val umlLabel = label
}