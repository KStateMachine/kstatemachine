package ru.nsk.kstatemachine.statemachine

import ru.nsk.kstatemachine.state.ChildMode
import ru.nsk.kstatemachine.state.DataState
import ru.nsk.kstatemachine.state.DefaultState
import ru.nsk.kstatemachine.state.IState

/**
 * Defines state machine API for internal library usage.
 */
internal abstract class InternalStateMachine(name: String?, childMode: ChildMode) :
    BuildingStateMachine, DefaultState(name, childMode) {
    internal abstract val areListenersMuted: Boolean
    internal abstract fun openListenersMutationSection(): ListenersMutationSection

    internal abstract suspend fun startFrom(states: Set<IState>, argument: Any?)
    internal abstract suspend fun <D : Any> startFrom(state: DataState<D>, data: D, argument: Any?)
    internal abstract fun delayListenerException(exception: Exception)
}