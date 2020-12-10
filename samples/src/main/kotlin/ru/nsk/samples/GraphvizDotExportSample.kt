package ru.nsk.samples

import ru.nsk.kstatemachine.*

object SwitchEvent : Event

fun main() {
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
