/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2026.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.metainfo

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private data class TagA(val v: Int) : MetaInfo
private data class TagB(val v: String) : MetaInfo
private data class TagC(val v: Boolean) : MetaInfo

class MetaInfoPlusTest : FreeSpec({
    "plus of two plain MetaInfo wraps them in a flat CompositeMetaInfo" {
        val a = TagA(1)
        val b = TagB("x")
        val result = a + b
        result!!.metaInfoSet shouldContainExactlyInAnyOrder setOf(a, b)
    }

    "plus unwraps a composite left operand" {
        val a = TagA(1)
        val b = TagB("x")
        val c = TagC(true)
        val composite = buildCompositeMetaInfo(a, b)

        val result = composite + c
        result.shouldBeInstanceOf<CompositeMetaInfo>()
        result.metaInfoSet shouldContainExactlyInAnyOrder setOf(a, b, c)
        // No nested CompositeMetaInfo
        result.metaInfoSet.none { it is CompositeMetaInfo } shouldBe true
    }

    "plus unwraps a composite right operand" {
        val a = TagA(1)
        val b = TagB("x")
        val c = TagC(true)
        val composite = buildCompositeMetaInfo(b, c)

        val result = a + composite
        result!!.metaInfoSet shouldContainExactlyInAnyOrder setOf(a, b, c)
        result.metaInfoSet.none { it is CompositeMetaInfo } shouldBe true
    }

    "plus unwraps both composite operands into one flat set" {
        val a = TagA(1)
        val b = TagB("x")
        val c = TagC(true)
        val left = buildCompositeMetaInfo(a, b)
        val right = buildCompositeMetaInfo(c, TagA(2))

        val result = left + right
        result!!.metaInfoSet shouldContainExactlyInAnyOrder setOf(a, b, c, TagA(2))
        result.metaInfoSet.none { it is CompositeMetaInfo } shouldBe true
    }

    "plus deduplicates equal MetaInfo instances across both operands" {
        val a = TagA(1)
        val b = TagB("x")
        val result = buildCompositeMetaInfo(a, b) + buildCompositeMetaInfo(a, TagB("x"))
        result!!.metaInfoSet shouldContainExactlyInAnyOrder setOf(a, b)
    }
})
