package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.*

internal class GetActiveLeafStatesVisitor : Visitor {
    private var _activeLeafStates = mutableSetOf<IState>()
    val activeLeafStates: Set<IState> get() = _activeLeafStates

    override fun visit(machine: StateMachine) = visit(machine as IState)

    override fun visit(state: IState) {
        state as InternalState
        if (!state.isActive) return

        val currentStates = state.getCurrentStates()
        if (currentStates.isEmpty()) { // state is a leaf
            _activeLeafStates.add(state)
        } else {
            for (currentState in currentStates) {
                // do not include nested state machine states
                if (currentState is StateMachine)
                    _activeLeafStates.add(currentState)
                else
                    visit(currentState)
            }
        }
    }

    override fun <E : Event> visit(transition: Transition<E>) = Unit // noting to do
}