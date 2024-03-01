package ru.nsk.samples

import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.metainfo.MetaInfo
import ru.nsk.kstatemachine.metainfo.UmlMetaInfo
import ru.nsk.kstatemachine.state.State
import ru.nsk.kstatemachine.state.finalState
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.transitionOn
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.visitors.export.exportToPlantUml
import ru.nsk.samples.PlantUmlExportWithMetaInfoSample.SwitchEvent

private object PlantUmlExportWithMetaInfoSample {
    object SwitchEvent : Event
}

/**
 * The sample shows hot to use [MetaInfo] to beautify export output
 */
fun main() = runBlocking {
    lateinit var state2: State
    val machine = createStateMachine(this) {
        metaInfo = UmlMetaInfo(umlLabel = "Nested states sm")

        initialState("State1") {
            metaInfo = UmlMetaInfo("State 1 Label")
            transitionOn<SwitchEvent> {
                metaInfo = UmlMetaInfo("Transition to State 2")
                targetState = { state2 }
            }
        }

        state2 = finalState("State2") {
            metaInfo = UmlMetaInfo(
                umlLabel = "FinalState 2 Label",
                umlStateDescriptions = listOf("Description 1", "Description 2"),
                umlNotes = listOf("Note 1", "Note 2"),
            )
        }
    }

    println(machine.exportToPlantUml(showEventLabels = true))
}