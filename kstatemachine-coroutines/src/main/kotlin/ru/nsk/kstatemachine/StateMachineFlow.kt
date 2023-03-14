package ru.nsk.kstatemachine

import kotlinx.coroutines.flow.*
import ru.nsk.kstatemachine.StateMachineNotification.*

sealed class StateMachineNotification(val machine: StateMachine) {
    class Started(machine: StateMachine) : StateMachineNotification(machine)
    class TransitionTriggered(
        val transitionParams: TransitionParams<*>,
        machine: StateMachine
    ) : StateMachineNotification(machine)

    class TransitionComplete(
        val transitionParams: TransitionParams<*>,
        val activeStates: Set<IState>,
        machine: StateMachine
    ) : StateMachineNotification(machine)

    class StateEntry(
        val state: IState,
        val transitionParams: TransitionParams<*>,
        machine: StateMachine
    ) : StateMachineNotification(machine)

    class StateExit(
        val state: IState,
        val transitionParams: TransitionParams<*>,
        machine: StateMachine
    ) : StateMachineNotification(machine)

    class StateFinished(
        val state: IState,
        val transitionParams: TransitionParams<*>,
        machine: StateMachine
    ) : StateMachineNotification(machine)

    class Stopped(machine: StateMachine) : StateMachineNotification(machine)
    class Destroyed(machine: StateMachine) : StateMachineNotification(machine)
}

fun StateMachine.stateMachineNotificationFlow(replay: Int = 0): SharedFlow<StateMachineNotification> {
    val machine = this
    val flow = MutableSharedFlow<StateMachineNotification>(replay = replay)
    addListener(object : StateMachine.Listener {
        override suspend fun onStarted() = flow.emit(Started(machine))
        override suspend fun onTransitionTriggered(transitionParams: TransitionParams<*>) =
            flow.emit(TransitionTriggered(transitionParams, machine))

        override suspend fun onTransitionComplete(transitionParams: TransitionParams<*>, activeStates: Set<IState>) =
            flow.emit(TransitionComplete(transitionParams, activeStates, machine))

        override suspend fun onStateEntry(state: IState, transitionParams: TransitionParams<*>) =
            flow.emit(StateEntry(state, transitionParams, machine))

        override suspend fun onStateExit(state: IState, transitionParams: TransitionParams<*>) =
            flow.emit(StateExit(state, transitionParams, machine))

        override suspend fun onStateFinished(state: IState, transitionParams: TransitionParams<*>) =
            flow.emit(StateFinished(state, transitionParams, machine))

        override suspend fun onStopped() = flow.emit(Stopped(machine))
        override suspend fun onDestroyed() = flow.emit(Destroyed(machine))
    })
    return flow.asSharedFlow()
}