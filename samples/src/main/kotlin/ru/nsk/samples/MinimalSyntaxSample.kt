package ru.nsk.samples

import ru.nsk.kstatemachine.*

// Define events
object YellowEvent : Event
object RedEvent : Event

fun main() {
    // Create state machine and configure its states in a setup block
    val stateMachine = createStateMachine {
        // State machine finishes when enters final state
        val redState = finalState()

        val yellowState = state {
            // Setup transition on RedEvent
            transition<RedEvent> {
                targetState = redState
                // Add transition listener
                onTriggered { println("Transition on ${it.event}") }
            }
        }

        initialState("Green") {
            // Add state listeners
            onEntry { println("Enter $name state") }
            onExit { println("Exit $name state") }

            transition<YellowEvent> { targetState = yellowState }
        }
    }

    // Process events
    stateMachine.processEvent(YellowEvent)
    stateMachine.processEvent(RedEvent)
}