package ru.nsk.samples

import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.visitors.exportToPlantUml
import ru.nsk.samples.PlantUmlExportWithMetaInfoSample.SwitchEvent

private object PlantUmlExportWithMetaInfoSample {
    object SwitchEvent : Event
}

/**
 * The sample shows hot to use [MetaInfo] to beautify export output
 */
fun main() = runBlocking {
    val machine = createStateMachine(this) {
        // label for state machine
        metaInfo = UmlMetaInfo("Nested states sm")

        val state1 = initialState("State1") {
            // label for state
            metaInfo = UmlMetaInfo("State 1")
        }
        val state3 = finalState("State3") {
            // label for state
            metaInfo = UmlMetaInfo("State 3")
        }

        val state2 = state("State2") {
            // label for state
            metaInfo = UmlMetaInfo("State 2")
            transition<SwitchEvent> {
                // label for transition
                metaInfo = UmlMetaInfo("That's all")
                targetState = state3
            }
            transition<SwitchEvent> {
                // label for transition
                metaInfo = UmlMetaInfo("back to State 1")
                targetState = state1
            }
            val finalSubState = finalState {
                // label for state
                metaInfo = UmlMetaInfo("Final sub state")
            }
            initialState("Initial subState") {
                transition<SwitchEvent> { targetState = finalSubState }
            }
        }

        state1 {
            transition<SwitchEvent> {
                metaInfo = UmlMetaInfo("go to ${state2.name}")
                targetState = state2
            }
            transition<SwitchEvent> { targetState = this@state1 }
            transition<SwitchEvent>()
        }
    }

    println(machine.exportToPlantUml())
}