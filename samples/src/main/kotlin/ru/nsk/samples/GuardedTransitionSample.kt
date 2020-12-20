package ru.nsk.samples

import ru.nsk.kstatemachine.*

object SwitchEvent1 : Event

sealed class States1 {
    class InitialState(val value: Int) : DefaultState("State 1")
    object FinalState : DefaultFinalState("State 2")
}

fun main() {
    val machine = createStateMachine {
        logger = StateMachine.Logger { println(it) }

        addInitialState(States1.InitialState(42)) {
            transitionTo<SwitchEvent1> {
                guard = { this@addInitialState.value > 10 }
                targetState = { States1.FinalState }
            }
        }

        addFinalState(States1.FinalState)
    }

    machine.processEvent(SwitchEvent1)
}