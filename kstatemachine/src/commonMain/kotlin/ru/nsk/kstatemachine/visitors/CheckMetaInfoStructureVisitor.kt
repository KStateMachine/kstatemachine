/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.metainfo.CompositeMetaInfo
import ru.nsk.kstatemachine.metainfo.MetaInfo
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.transition.Transition

/**
 * Checks that [MetaInfo] is correctly structured.
 * [CompositeMetaInfo] is not nested into each other.
 * Certain [MetaInfo] subclasses are applied only once.
 */
internal class CheckMetaInfoStructureVisitor : RecursiveVisitor {
    override fun visit(machine: StateMachine) {
        machine.metaInfo?.checkStructure()
        machine.visitChildren()
    }

    override fun visit(state: IState) {
        state.metaInfo?.checkStructure()

        if (state !is StateMachine) // do not check nested machines
            state.visitChildren()
    }

    override fun <E : Event> visit(transition: Transition<E>) {
        transition.metaInfo?.checkStructure()
    }

    private fun MetaInfo.checkStructure() {
        if (this is CompositeMetaInfo) {
            val groups = metaInfoSet.groupBy {
                check(it !is CompositeMetaInfo) { "CompositeMetaInfo cannot nest each other" }
                it::class
            }
            groups.entries.forEach {
                check(it.value.size == 1) { "MetaInfo ${it.key} is repeated more than once" }
            }
        }
    }
}