package ru.nsk.kstatemachine

import org.junit.jupiter.api.Test

class StateMachineExportTest {
    @Test
    fun exportToDot() {
        val stateMachine = createStateMachine("Traffic lights") {
            val greenState = initialState("Green")
            val redState = finalState("Red")

            val yellowState = state("Yellow") {
                transition<SwitchEvent> { targetState = redState }
                transition<SwitchEvent>("back") { targetState = greenState }
            }

            greenState {
                transition<SwitchEvent>("to yellow") { targetState = yellowState }
                transition<SwitchEvent> { targetState = this@greenState }
                transition<SwitchEvent>()
            }
        }

        val dot = stateMachine.exportToDot()
        println(dot)
    }
}