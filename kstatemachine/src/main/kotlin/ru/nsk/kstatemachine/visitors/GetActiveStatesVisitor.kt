package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.*

internal class GetActiveStatesVisitor(private val selfIncluding: Boolean) : Visitor {
    private var _activeStates = mutableSetOf<IState>()
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

//override fun recursiveFillActiveStates(states: MutableSet<IState>, self: IState, selfIncluding: Boolean) {
//    if (!_isActive) return
//    if (this == self) {
//        if (selfIncluding) states.add(this)
//    } else {
//        states.add(this)
//    }
//
//    for (currentState in getCurrentStates()) {
//        // do not include nested state machine states
//        if (currentState is StateMachine)
//            states.add(currentState)
//        else
//            currentState.recursiveFillActiveStates(states, self, selfIncluding)
//    }
//}