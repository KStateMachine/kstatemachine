/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.samples

import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.samples.InheritTransitionsSample.Events.ExitEvent
import ru.nsk.samples.InheritTransitionsSample.Events.SwitchEvent

private object InheritTransitionsSample {
    sealed interface Events : Event {
        data object ExitEvent : Events
        data object SwitchEvent : Events
    }
}

/**
 * Nested states allow grouping states and inherit their parent transitions
 */
fun main() = runBlocking {
    val machine = createStateMachine(this, name = "Nested states") {
        logger = StateMachine.Logger { println(it()) }

        val state2 = finalState("State2")

        initialState("State1") {
            transition<ExitEvent>("Exit") { targetState = state2 }

            val state11 = initialState("State1_1")
            val state12 = state("State1_2")
            val state13 = state("State1_3")

            state11 {
                transition<SwitchEvent> { targetState = state12 }
            }
            state12 {
                transition<SwitchEvent> { targetState = state13 }
            }
            state13 {
                transition<SwitchEvent> { targetState = state11 }
            }
        }
    }

    machine.processEvent(SwitchEvent)
    machine.processEvent(SwitchEvent)
    machine.processEvent(ExitEvent)

    check(machine.requireState("State2") in machine.activeStates())
}