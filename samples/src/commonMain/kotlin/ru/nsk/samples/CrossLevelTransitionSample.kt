package ru.nsk.samples

import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.State
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.state
import ru.nsk.kstatemachine.state.transitionOn
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.visitors.export.exportToPlantUml
import ru.nsk.samples.CrossLevelTransitionSample.SwitchEvent

private object CrossLevelTransitionSample {
    object SwitchEvent : Event
}

/**
 * Transition may target any states from state machine
 */
fun main() = runBlocking {
    val machine = createStateMachine(this) {
        logger = StateMachine.Logger { println(it()) }

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