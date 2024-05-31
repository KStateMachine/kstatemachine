/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.transition.Transition

internal class CheckUniqueNamesVisitor : RecursiveVisitor {
    private val stateNames = mutableSetOf<String>()
    private val transitionNames = mutableSetOf<String>()

    override fun visit(machine: StateMachine) {
        machine.name?.let { check(stateNames.add(it)) { "State name is not unique: $it" } }
        machine.visitChildren()
    }

    override fun visit(state: IState) {
        state.name?.let { check(stateNames.add(it)) { "State name is not unique: $it" } }
        if (state !is StateMachine) // do not check nested machines
            state.visitChildren()
    }

    override fun <E : Event> visit(transition: Transition<E>) {
        transition.name?.let { check(transitionNames.add(it)) { "Transition name is not unique: $it" } }
    }
}