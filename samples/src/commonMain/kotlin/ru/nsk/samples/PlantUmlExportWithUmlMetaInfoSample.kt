/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.samples

import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.metainfo.UmlMetaInfo
import ru.nsk.kstatemachine.metainfo.buildUmlMetaInfo
import ru.nsk.kstatemachine.state.finalState
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.visitors.export.exportToPlantUml
import ru.nsk.samples.PlantUmlExportWithUmlMetaInfoSample.SwitchEvent

private object PlantUmlExportWithUmlMetaInfoSample {
    object SwitchEvent : Event
}

/**
 * The sample shows how to use [UmlMetaInfo] to beautify export output
 */
fun main() = runBlocking {
    val machine = createStateMachine(this) {
        metaInfo = buildUmlMetaInfo { umlLabel = "Nested states sm" }

        val state2 = finalState("State2") {
            metaInfo = buildUmlMetaInfo {
                umlLabel = "FinalState 2 Label"
                umlStateDescriptions = listOf("Description 1", "Description 2")
                umlNotes = listOf("Note 1", "Note 2")
            }
        }

        initialState("State1") {
            metaInfo = buildUmlMetaInfo { umlLabel = "State 1 Label" }
            transition<SwitchEvent> {
                metaInfo = buildUmlMetaInfo { umlLabel = "Transition to State 2" }
                targetState = state2
            }
        }
    }

    println(machine.exportToPlantUml(showEventLabels = true))
}