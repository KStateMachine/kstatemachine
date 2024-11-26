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
 * Additional static (designed to be immutable) info for library primitives like [IState] [Transition] etc.
 * Users may extend this interface to add their own [MetaInfo] implementations.
 * Users may combine multiple [MetaInfo] derived interfaces into single object or use [CompositeMetaInfo] instead.
 */
interface MetaInfo

/**
 * Allows to specify multiple [MetaInfo] objects.
 * It might be simpler than constructing single object implementing multiple [MetaInfo] derived interfaces.
 * Nesting [CompositeMetaInfo] into each other is not supported.
 */
interface CompositeMetaInfo : MetaInfo {
    /**
     * Default: emptySet()
     */
    val metaInfoSet: Set<MetaInfo>
}

internal inline fun <reified M : MetaInfo> MetaInfo.findMetaInfo(): M? {
    return when (this) {
        is M -> this
        is CompositeMetaInfo -> metaInfoSet.singleOrNull { it is M } as? M
        else -> null
    }
}

interface CompositeMetaInfoBuilder : CompositeMetaInfo {
    override var metaInfoSet: Set<MetaInfo>
}

private data class CompositeMetaInfoBuilderImpl(
    override var metaInfoSet: Set<MetaInfo> = emptySet()
) : CompositeMetaInfoBuilder

fun buildCompositeMetaInfo(builder: CompositeMetaInfoBuilder.() -> Unit): CompositeMetaInfo =
    CompositeMetaInfoBuilderImpl().apply(builder).copy()

fun buildCompositeMetaInfo(metaInfo1: MetaInfo, metaInfo2: MetaInfo, vararg infos: MetaInfo): CompositeMetaInfo =
    CompositeMetaInfoBuilderImpl(infos.toMutableSet().apply {
        add(metaInfo1)
        add(metaInfo2)
    })

