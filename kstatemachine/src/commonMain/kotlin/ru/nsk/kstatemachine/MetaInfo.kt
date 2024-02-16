package ru.nsk.kstatemachine

interface MetaInfo
interface UmlMetaInfo: MetaInfo {
    val umlLabel: String
}
fun umlLabel(label: String) = object : UmlMetaInfo {
    override val umlLabel = label
}