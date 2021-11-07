package ru.nsk.samples

import ru.nsk.kstatemachine.*
import ru.nsk.samples.Events.NextEvent
import ru.nsk.samples.States.*
import ru.nsk.samples.States.YellowState

sealed class Events {
    object NextEvent : Event
}

sealed class States {
    object GreenState : DefaultState()
    object YellowState : DefaultState()
    object RedState : DefaultFinalState() // Machine finishes when enters final state
}

fun main() {
    // Create state machine and configure its states in a setup block
    val machine = createStateMachine {
        addInitialState(GreenState) {
            // Add state listeners
            onEntry { println("Enter $this") }
            onExit { println("Exit $this") }

            // Setup transition
            transition<NextEvent> {
                targetState = YellowState
                // Add transition listener
                onTriggered { println("Transition triggered") }
            }
        }

        addState(YellowState) {
            transition<NextEvent>(targetState = RedState)
        }

        addFinalState(RedState)

        onFinished { println("Finished") }
    }

    // Now we can process events
    machine.processEvent(NextEvent)
    machine.processEvent(NextEvent)
}