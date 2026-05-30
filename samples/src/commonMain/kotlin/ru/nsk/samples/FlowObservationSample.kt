/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.samples

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.StateMachineNotification
import ru.nsk.kstatemachine.statemachine.activeStatesFlow
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.statemachine.stateMachineNotificationFlow

private object FlowObservationSample {
    object SwitchEvent : Event
}

/**
 * [activeStatesFlow] exposes the set of currently active states as a [kotlinx.coroutines.flow.StateFlow].
 * [stateMachineNotificationFlow] exposes all machine lifecycle events as a [kotlinx.coroutines.flow.SharedFlow].
 * Both are available from the [ru.nsk.kstatemachine.statemachine] package in [kstatemachine-coroutines].
 */
fun main(): Unit = runBlocking {
    val machine = createStateMachine(this) {
        logger = StateMachine.Logger { println(it()) }

        val state2 = state("state2") {
            onEntry { println("Entered state2") }
        }

        initialState("state1") {
            transition<FlowObservationSample.SwitchEvent> { targetState = state2 }
        }
    }

    // observe active states reactively
    machine.activeStatesFlow()
        .onEach { states -> println("Active states: ${states.map { it.name }}") }
        .launchIn(this)

    // observe machine notifications (transitions, entries, exits, etc.)
    machine.stateMachineNotificationFlow(extraBufferCapacity = 10)
        .take(3)
        .onEach { notification ->
            when (notification) {
                is StateMachineNotification.TransitionTriggered ->
                    println("Transition triggered by: ${notification.transitionParams.event}")
                is StateMachineNotification.StateEntry ->
                    println("Entered: ${notification.state.name}")
                is StateMachineNotification.StateExit ->
                    println("Exited: ${notification.state.name}")
                else -> Unit
            }
        }
        .launchIn(this)

    machine.processEvent(FlowObservationSample.SwitchEvent)
}
