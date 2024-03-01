package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.state.InternalState
import ru.nsk.kstatemachine.statemachine.InternalStateMachine
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.transition.Transition

internal class CleanupVisitor : RecursiveCoVisitor {
    override suspend fun visit(machine: StateMachine) {
        machine.visitChildren()
        (machine as InternalStateMachine).cleanup()
    }

    override suspend fun visit(state: IState) {
        if (state !is StateMachine) { // do not visit nested machines
            state.visitChildren()
            (state as InternalState).cleanup()
        }
    }

    override suspend fun <E : Event> visit(transition: Transition<E>) = Unit // nothing to do
}