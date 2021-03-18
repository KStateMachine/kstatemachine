package ru.nsk.samples

import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.visitors.exportToPlantUml
import java.lang.System.lineSeparator

object ExitEvent : UnitEvent()
object NextEvent : UnitEvent()

/**
 * Nested states allow us to group states and inherit its parent transitions
 */
fun main() {
    val machine = createStateMachine("Nested states") {
        logger = StateMachine.Logger { println(it) }

        val state2 = finalState("Finish")

        initialState("State1") {
            transition<ExitEvent>("Exit") { targetState = state2 }

            val state11 = initialState("State1_1")
            val state12 = state("State1_2")
            val state13 = state("State1_3")

            state11 {
                transition<NextEvent> { targetState = state12 }
            }
            state12 {
                transition<NextEvent> { targetState = state13 }
            }
            state13 {
                transition<NextEvent> { targetState = state11 }
            }
        }
    }

    machine.processEvent(NextEvent)
    machine.processEvent(NextEvent)
    machine.processEvent(ExitEvent)

    println(lineSeparator() + machine.exportToPlantUml())
}