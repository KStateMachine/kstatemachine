package ru.nsk.samples

import ru.nsk.kstatemachine.*

object SwitchEvent1 : Event

sealed class States1 {
    class InitialState(val value: Int) : DefaultState("State 1")
    class FinalState : DefaultFinalState("State 2")
}

fun main() {
    val machine = createStateMachine {
        logger = StateMachine.Logger { println(it) }

        val finalState = addFinalState(States1.FinalState())

        addInitialState(States1.InitialState(42)) {
            transition<SwitchEvent1> {
                guard = { this@addInitialState.value > 10 }
                targetState = finalState
            }
        }
    }

    machine.processEvent(SwitchEvent1)
}