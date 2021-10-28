package ru.nsk.samples

import ru.nsk.kstatemachine.*

// Define events
sealed class Events {
    object YellowEvent : Event
    object RedEvent : Event
}

// Define states
sealed class States {
    object GreenState : DefaultState("Green")
    object YellowState : DefaultState("Yellow")
    object RedState : DefaultFinalState("Red") // State machine finishes when enters final state
}

fun main() {
    // Create state machine and configure its states in a setup block
    val machine = createStateMachine {
        addInitialState(States.GreenState) {
            // Add state listeners
            onEntry { println("Enter $name state") }
            onExit { println("Exit $name state") }

            // Setup transition on YellowEvent
            transition<Events.YellowEvent> {
                targetState = States.YellowState
                // Add transition listener
                onTriggered { println("Transition on ${it.event::class.simpleName}") }
            }
        }

        addState(States.YellowState) {
            transition<Events.RedEvent> { targetState = States.RedState }
        }

        addFinalState(States.RedState)
    }

    // Process events
    machine.processEvent(Events.YellowEvent)
    machine.processEvent(Events.RedEvent)
}