/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.metainfo

import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.transition.Transition
import kotlin.collections.single

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
 * Only one instance of certain [MetaInfo] subtype should be specified.
 */
interface CompositeMetaInfo : MetaInfo {
    /**
     * Default: emptySet()
     */
    val metaInfoSet: Set<MetaInfo>
}

/**
 * Helper method for getting [MetaInfo] of specified type
 */
inline fun <reified M : MetaInfo> MetaInfo?.findMetaInfo(): M? {
    return when (this) {
        is M -> this
        is CompositeMetaInfo -> {
            val infos = metaInfoSet.filterIsInstance<M>()
            when (infos.size) {
                0 -> null
                1 -> infos.single()
                else -> throw IllegalArgumentException("MetaInfo set has more than one element of type ${M::class.simpleName}")
            }
        }
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

