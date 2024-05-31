/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.statemachine

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import ru.nsk.kstatemachine.statemachine.StateMachineNotification.*
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.state.activeStates
import ru.nsk.kstatemachine.transition.TransitionParams

sealed class StateMachineNotification(val machine: StateMachine) {
    class Started(
        val transitionParams: TransitionParams<*>,
        machine: StateMachine
    ) : StateMachineNotification(machine)

    class TransitionTriggered(
        val transitionParams: TransitionParams<*>,
        machine: StateMachine
    ) : StateMachineNotification(machine)

    class TransitionComplete(
        val activeStates: Set<IState>,
        val transitionParams: TransitionParams<*>,
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

fun StateMachine.stateMachineNotificationFlow(
    replay: Int = 0,
    extraBufferCapacity: Int = 0,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND
): SharedFlow<StateMachineNotification> {
    val machine = this
    val flow = MutableSharedFlow<StateMachineNotification>(
        replay = replay,
        extraBufferCapacity = extraBufferCapacity,
        onBufferOverflow
    )
    addListener(object : StateMachine.Listener {
        override suspend fun onStarted(transitionParams: TransitionParams<*>) =
            flow.emit(Started(transitionParams, machine))

        override suspend fun onTransitionTriggered(transitionParams: TransitionParams<*>) =
            flow.emit(TransitionTriggered(transitionParams, machine))

        override suspend fun onTransitionComplete(activeStates: Set<IState>, transitionParams: TransitionParams<*>) =
            flow.emit(TransitionComplete(activeStates, transitionParams, machine))

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

/**
 * Provides active states as [Flow]
 */
fun StateMachine.activeStatesFlow(): StateFlow<Set<IState>> {
    val flow = MutableStateFlow(activeStates())
    onTransitionComplete { activeStates, _ -> flow.emit(activeStates) }
    return flow.asStateFlow()
}