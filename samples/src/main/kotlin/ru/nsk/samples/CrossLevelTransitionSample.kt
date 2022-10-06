package ru.nsk.samples

import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.visitors.exportToPlantUml
import ru.nsk.samples.CrossLevelTransitionSample.SwitchEvent

private object CrossLevelTransitionSample {
    object SwitchEvent : Event
}

/**
 * Transition may target any states from state machine
 */
fun main() {
    val machine = createStateMachine {
        logger = StateMachine.Logger { println(it) }

        lateinit var nested22: State

        initialState("Top level 1") {
            initialState("Nested 11") {
                // Target state is declared in another state
                transitionOn<SwitchEvent> { targetState = { nested22 } }
            }
        }

        state("Top level 2") {
            initialState("Nested 21")
            nested22 = state("Nested 22")
        }
    }

    machine.processEvent(SwitchEvent)

    println("\n" + machine.exportToPlantUml())
}