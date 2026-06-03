/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.statemachine.NonBlankNamesRequirement
import ru.nsk.kstatemachine.statemachine.NonBlankNamesRequirement.STATES
import ru.nsk.kstatemachine.statemachine.NonBlankNamesRequirement.STATES_AND_TRANSITIONS
import ru.nsk.kstatemachine.statemachine.NonBlankNamesRequirement.TRANSITIONS
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.transition.Transition

/**
 * Checks that machine contains states and transitions with filled and non-blank names.
 */
internal class RequireNonBlankNamesVisitor(private val requirement: NonBlankNamesRequirement) : RecursiveVisitor {
    private val invalidStates = mutableSetOf<IState>()
    private val invalidTransitions = mutableSetOf<Transition<*>>()

    override fun visit(machine: StateMachine) {
        if (machine.name.isNullOrBlank())
            invalidStates += machine
        machine.visitChildren()
    }

    override fun visit(state: IState) {
        if (requirement in listOf(STATES, STATES_AND_TRANSITIONS) && state.name.isNullOrBlank())
            invalidStates += state

        if (state !is StateMachine) // do not check nested machines
            state.visitChildren()
    }

    override fun <E : Event> visit(transition: Transition<E>) {
        if (requirement in listOf(TRANSITIONS, STATES_AND_TRANSITIONS) && transition.name.isNullOrBlank())
            invalidTransitions += transition
    }

    fun hasBlankNames() = invalidStates.isNotEmpty() || invalidTransitions.isNotEmpty()

    fun checkNonBlankNames() {
        check(!hasBlankNames()) {
            val statesText = invalidStates.joinToString { "$it (child of ${it.parent})" }
            val transitionsText = invalidTransitions.joinToString { "$it (in ${it.sourceState})" }
            "There were blank names in states: $statesText transitions: $transitionsText"
        }
    }
}

fun StateMachine.hasBlankNames(requirement: NonBlankNamesRequirement = STATES_AND_TRANSITIONS): Boolean {
    val visitor = RequireNonBlankNamesVisitor(requirement)
    accept(visitor)
    return visitor.hasBlankNames()
}

fun StateMachine.checkNonBlankNames(requirement: NonBlankNamesRequirement = STATES_AND_TRANSITIONS) {
    val visitor = RequireNonBlankNamesVisitor(requirement)
    accept(visitor)
    visitor.checkNonBlankNames()
}