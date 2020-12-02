package ru.nsk.samples

import ru.nsk.kstatemachine.*

// Define events
object SwitchGreenEvent1 : Event
object SwitchYellowEvent1 : Event
object SwitchRedEvent1 : Event

fun main() {
    // Setup state machine
    val stateMachine = createStateMachine {
        // Setup states
        val greenState = initialState("Green")
        val yellowState = state()
        val redState = state {
            // Setup transitions
            transition<SwitchGreenEvent1> { targetState = greenState }
        }

        // Configure states
        greenState {
            onEntry { println("Enter $name state") }
            onExit { println("Exit $name state") }
            transition<SwitchYellowEvent1> { targetState = yellowState }
        }

        yellowState {
            transition<SwitchRedEvent1> {
                targetState = redState
                onTriggered { println("Transition on ${it.event}") }
            }
        }
    }

    // Process events
    stateMachine.processEvent(SwitchYellowEvent1)
    stateMachine.processEvent(SwitchRedEvent1)
    stateMachine.processEvent(SwitchGreenEvent1)
}