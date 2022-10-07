package ru.nsk.samples

import ru.nsk.kstatemachine.*
import ru.nsk.samples.MinimalSyntaxSample.SwitchEvent

private object MinimalSyntaxSample {
    object SwitchEvent : Event
}

/**
 * This sample uses factory functions to create states
 */
fun main() {
    // Create state machine and configure its states in a setup block
    val machine = createStateMachine {
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