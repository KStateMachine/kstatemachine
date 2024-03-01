package ru.nsk.samples

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.createStdLibStateMachine
import ru.nsk.kstatemachine.statemachine.processEventBlocking
import ru.nsk.kstatemachine.transition.onTriggered
import ru.nsk.samples.StdLibMinimalSealedClassesSample.States.*
import ru.nsk.samples.StdLibMinimalSealedClassesSample.SwitchEvent

private object StdLibMinimalSealedClassesSample {
    object SwitchEvent : Event

    sealed class States : DefaultState() {
        object GreenState : States()
        object YellowState : States()
        object RedState : States(), FinalState // Machine finishes when enters final state
    }
}

/**
 * This sample uses KStateMachine only with Kotlin Standard library (without Kotlin Coroutines library).
 */
fun main() {
    // Create state machine and configure its states in a setup block
    val machine = createStdLibStateMachine {
        addInitialState(GreenState) {
            // Add state listeners
            onEntry { println("Enter $this") }
            onExit { println("Exit $this") }

            // Setup transition
            transition<SwitchEvent> {
                targetState = YellowState
                // Add transition listener
                onTriggered { println("Transition triggered") }
            }
        }

        addState(YellowState) {
            transition<SwitchEvent>(targetState = RedState)
        }

        addFinalState(RedState)

        onFinished { println("Finished") }
    }

    // Now we can process events
    machine.processEventBlocking(SwitchEvent)
    machine.processEventBlocking(SwitchEvent)

    check(machine.isFinished)
}