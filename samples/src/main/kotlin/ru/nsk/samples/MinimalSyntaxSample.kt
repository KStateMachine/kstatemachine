package ru.nsk.samples

import ru.nsk.kstatemachine.*

object NextEvent1 : Event

fun main() {
    // Create state machine and configure its states in a setup block
    val machine = createStateMachine {
        // State machine finishes when enters final state
        val redState = finalState()

        val yellowState = state {
            // Setup transition
            transition<NextEvent1> {
                targetState = redState
                // Add transition listener
                onTriggered { println("Transition triggered") }
            }
        }

        initialState("green") {
            // Add state listeners
            onEntry { println("Enter $name") }
            onExit { println("Exit $name") }

            transition<NextEvent1>(targetState = yellowState)
        }

        onFinished { println("Finished") }
    }

    // Now we can process events
    machine.processEvent(NextEvent1)
    machine.processEvent(NextEvent1)
}