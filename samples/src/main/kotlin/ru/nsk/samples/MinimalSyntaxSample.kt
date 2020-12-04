package ru.nsk.samples

import ru.nsk.kstatemachine.*

// Define events
object YellowEvent : Event
object RedEvent : Event

fun main() {
    // Setup state machine
    val stateMachine = createStateMachine {
        // Create and configure states
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