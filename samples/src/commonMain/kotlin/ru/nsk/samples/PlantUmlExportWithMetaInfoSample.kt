package ru.nsk.samples

import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.visitors.exportToPlantUml
import ru.nsk.samples.PlantUmlExportWithMetaInfoSample.SwitchEvent

private object PlantUmlExportWithMetaInfoSample {
    object SwitchEvent : Event
}

fun main() = runBlocking {
    val machine = createStateMachine(this) {
        // label for state machine
        metaInfo = umlLabel("Nested states sm")

        val state1 = initialState("State1") {
            // label for state
            metaInfo = umlLabel("State 1")
        }
        val state3 = finalState("State3") {
            // label for state
            metaInfo = umlLabel("State 3")
        }

        val state2 = state("State2") {
            // label for state
            metaInfo = umlLabel("State 2")
            transition<SwitchEvent> {
                // label for transition
                metaInfo = umlLabel("That's all")
                targetState = state3
            }
            transition<SwitchEvent> {
                // label for transition
                metaInfo = umlLabel("back to State 1")
                targetState = state1
            }
            val finalSubState = finalState {
                // label for state
                metaInfo = umlLabel("Final sub state")
            }
            initialState("Initial subState") {
                transition<SwitchEvent> { targetState = finalSubState }
            }
        }

        state1 {
            transition<SwitchEvent> {
                metaInfo = umlLabel("go to ${state2.name}")
                targetState = state2
            }
            transition<SwitchEvent> { targetState = this@state1 }
            transition<SwitchEvent>()
        }
    }

    println(machine.exportToPlantUml())
}