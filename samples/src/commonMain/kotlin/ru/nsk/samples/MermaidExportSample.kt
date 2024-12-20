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
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.visitors.export.exportToMermaid
import ru.nsk.samples.MermaidExportSample.SwitchEvent

private object MermaidExportSample {
    object SwitchEvent : Event
}

/**
 * The sample shows basic Mermaid export
 */
fun main() = runBlocking {
    val machine = createStateMachine(this, name = "Nested states") {
        val state1 = initialState("State1")
        val state3 = finalState("State3")

        val state2 = state("State2") {
            transition<SwitchEvent> { targetState = state3 }
            transition<SwitchEvent>("back") { targetState = state1 }

            val finalSubState = finalState("Final subState")
            initialState("Initial subState") {
                transition<SwitchEvent> { targetState = finalSubState }
            }
        }

        state1 {
            transition<SwitchEvent>("to ${state2.name}") { targetState = state2 }
            transition<SwitchEvent> { targetState = this@state1 }
            transition<SwitchEvent>()
        }
    }

    println(machine.exportToMermaid())
}
