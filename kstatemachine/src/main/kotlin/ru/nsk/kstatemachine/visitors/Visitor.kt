package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.State
import ru.nsk.kstatemachine.StateMachine
import ru.nsk.kstatemachine.Transition

/**
 * Interface for visiting state machine components
 */
interface Visitor {
    fun visit(machine: StateMachine)
    fun visit(state: State)
    fun visit(transition: Transition<*>)
}

interface VisitorAcceptor {
    fun accept(visitor: Visitor)
}
