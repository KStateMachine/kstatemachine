package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.*

internal class CleanupVisitor : RecursiveVisitor {

    override fun visit(machine: StateMachine) {
        machine.visitChildren()
        (machine as InternalStateMachine).cleanup()
    }

    override fun visit(state: IState) {
        if (state !is StateMachine) { // do not visit nested machines
            state.visitChildren()
            (state as InternalState).cleanup()
        }
    }

    override fun <E : Event> visit(transition: Transition<E>) = Unit // nothing to do
}