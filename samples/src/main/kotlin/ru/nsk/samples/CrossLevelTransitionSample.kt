package ru.nsk.samples

import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.visitors.exportToPlantUml


object SwitchEvent2 : Event

fun main() {
    val machine = createStateMachine {
        logger = StateMachine.Logger { println(it) }

        lateinit var state22: State

        initialState("1") {
            initialState("11") {
                transitionTo<SwitchEvent2> { targetState = { state22 } }
            }
        }

        state("2") {
            initialState("21")
            state22 = state("22")
        }
    }

    machine.processEvent(SwitchEvent2)

    println(System.lineSeparator() + machine.exportToPlantUml())
}