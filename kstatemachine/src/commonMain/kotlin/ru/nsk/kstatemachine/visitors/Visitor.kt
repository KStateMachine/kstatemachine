package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.transition.Transition

/**
 * Suspendable interface for visiting state machine components
 * Visitor must be used instead of extension functions to preserve virtual behaviour, which is missing with extensions.
 */
interface CoVisitor {
    suspend fun visit(machine: StateMachine)
    suspend fun visit(state: IState)
    suspend fun <E : Event> visit(transition: Transition<E>)
}

/**
 * Interface for visiting state machine components without suspension
 */
interface Visitor {
    fun visit(machine: StateMachine)
    fun visit(state: IState)
    fun <E : Event> visit(transition: Transition<E>)
}

interface VisitorAcceptor {
    suspend fun accept(visitor: CoVisitor)
    fun accept(visitor: Visitor)
}