package ru.nsk.samples

import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.visitors.exportToPlantUml
import ru.nsk.samples.InheritTransitionsSample.Events.ExitEvent
import ru.nsk.samples.InheritTransitionsSample.Events.NextEvent

private object InheritTransitionsSample {
    sealed interface Events: Event {
        object ExitEvent : Events
        object NextEvent : Events
    }
}

/**
 * Nested states allow grouping states and inherit their parent transitions
 */
fun main() {
    val machine = createStateMachine("Nested states") {
        logger = StateMachine.Logger { println(it) }

        val state2 = finalState("State2")

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

    check(machine.requireState("State2") in machine.activeStates())

    println("\n" + machine.exportToPlantUml())
}