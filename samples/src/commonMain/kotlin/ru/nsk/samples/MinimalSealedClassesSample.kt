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
import ru.nsk.samples.MinimalSealedClassesSample.States.*
import ru.nsk.samples.MinimalSealedClassesSample.SwitchEvent

private object MinimalSealedClassesSample {
    object SwitchEvent : Event

    sealed class States : DefaultState() {
        object GreenState : States()
        object YellowState : States()
        object RedState : States(), FinalState // Machine finishes when enters final state
    }
}

/**
 * This sample uses states defined in sealed class
 */
fun main() = runBlocking {
    // Create state machine and configure its states in a setup block
    val machine = createStateMachine(this) {
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
    machine.processEvent(SwitchEvent)
    machine.processEvent(SwitchEvent)

    check(machine.isFinished)
}