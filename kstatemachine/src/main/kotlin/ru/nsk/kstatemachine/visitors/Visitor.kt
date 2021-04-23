package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.IState
import ru.nsk.kstatemachine.StateMachine
import ru.nsk.kstatemachine.Transition

/**
 * Interface for visiting state machine components
 */
interface Visitor {
    fun visit(machine: StateMachine)
    fun visit(state: IState)
    fun visit(transition: Transition<*>)
}

interface VisitorAcceptor {
    fun accept(visitor: Visitor)
}
