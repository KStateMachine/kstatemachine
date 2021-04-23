package ru.nsk.samples

import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.visitors.exportToDot

object SwitchEvent : Event

fun main() {
    val machine = createStateMachine("Traffic lights") {
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

    val dot = machine.exportToDot()
    println(dot)
}
