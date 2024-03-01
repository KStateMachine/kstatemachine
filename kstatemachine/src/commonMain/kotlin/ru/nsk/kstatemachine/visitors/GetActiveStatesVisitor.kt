package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.state.InternalState
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.transition.Transition

internal class GetActiveStatesVisitor(private val selfIncluding: Boolean) : Visitor {
    private val _activeStates = mutableSetOf<IState>()
    private var runner: IState? = null
    val activeStates: Set<IState> get() = _activeStates

    override fun visit(machine: StateMachine) = visit(machine as IState)

    override fun visit(state: IState) {
        state as InternalState
        if (!state.isActive) return

        if (runner == null) {
            runner = state
            if (selfIncluding) _activeStates.add(state)
        } else {
            _activeStates.add(state)
        }

        for (currentState in state.getCurrentStates()) {
            // do not include nested state machine states
            if (currentState is StateMachine)
                _activeStates.add(currentState)
            else
                visit(currentState)
        }
    }

    override fun <E : Event> visit(transition: Transition<E>) = Unit // noting to do
}