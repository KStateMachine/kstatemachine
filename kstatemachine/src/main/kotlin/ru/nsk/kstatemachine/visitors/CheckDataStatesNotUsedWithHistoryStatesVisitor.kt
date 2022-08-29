package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.*

internal class CheckDataStatesNotUsedWithHistoryStatesVisitor : RecursiveVisitor {
    private var hasDataStates = false
    private var hasHistoryStates = false

    override fun visit(machine: StateMachine) {
        machine.visitChildren()
    }

    override fun visit(state: IState) {
        when (state) {
            is DataState<*> -> hasDataStates = true
            is HistoryState -> hasHistoryStates = true
        }
        check(!(hasDataStates && hasHistoryStates)) {
            "It is not possible to use DataStates and HistoryStates in same machine"
        }
        if (state !is StateMachine) // do not check nested machines
            state.visitChildren()
    }

    override fun <E : Event> visit(transition: Transition<E>) = Unit
}