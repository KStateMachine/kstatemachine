package ru.nsk.samples

import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.visitors.exportToPlantUml
import ru.nsk.samples.PlantUmlExportSample.SwitchEvent

private object PlantUmlExportSample {
    object SwitchEvent : Event
}

fun main() = runBlocking {
    val machine = createStateMachine(this, metaInfo = umlLabel("Nested states")) {
        val state1 = initialState("State1", metaInfo = umlLabel("State 1"))
        val state3 = finalState("State3", metaInfo = umlLabel("State 3"))

        val state2 = state("State2", metaInfo = umlLabel("State 2")) {
            transition<SwitchEvent>(metaInfo = umlLabel("That's all")) { targetState = state3 }
            transition<SwitchEvent>(metaInfo = umlLabel("back to State 1")) { targetState = state1 }

            val finalSubState = finalState(metaInfo = umlLabel("Final sub state"))
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

    println(machine.exportToPlantUml())
}
