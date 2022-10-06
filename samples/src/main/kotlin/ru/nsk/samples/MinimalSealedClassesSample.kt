package ru.nsk.samples

import ru.nsk.kstatemachine.*
import ru.nsk.samples.MinimalSealedClassesSample.NextEvent
import ru.nsk.samples.MinimalSealedClassesSample.States.*

private object MinimalSealedClassesSample {
    object NextEvent : Event

    sealed class States : DefaultState() {
        object GreenState : States()
        object YellowState : States()
        object RedState : States(), FinalState // Machine finishes when enters final state
    }
}

/**
 * This sample uses states defined in sealed class
 */
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

    check(machine.isFinished)
}