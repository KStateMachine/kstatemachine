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
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.transition.onTriggered
import ru.nsk.samples.MinimalSyntaxSample.SwitchEvent

private object MinimalSyntaxSample {
    object SwitchEvent : Event
}

/**
 * This sample uses factory functions to create states
 */
fun main() = runBlocking {
    // Create state machine and configure its states in a setup block
    val machine = createStateMachine(this) {
        // State machine finishes when enters final state
        val redState = finalState()

        val yellowState = state {
            // Setup transition
            transition<SwitchEvent> {
                targetState = redState
                // Add transition listener
                onTriggered { println("Transition triggered") }
            }
        }

        initialState("green") {
            // Add state listeners
            onEntry { println("Enter $name") }
            onExit { println("Exit $name") }

            transition<SwitchEvent>(targetState = yellowState)
        }

        onFinished { println("Finished") }
    }

    // Now we can process events
    machine.processEvent(SwitchEvent)
    machine.processEvent(SwitchEvent)

    check(machine.isFinished)
}