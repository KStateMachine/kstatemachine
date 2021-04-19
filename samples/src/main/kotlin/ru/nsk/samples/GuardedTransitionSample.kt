package ru.nsk.samples

import ru.nsk.kstatemachine.*

object SwitchEvent1 : UnitEvent

sealed class States1 {
    class InitialState(val value: Int) : DefaultUnitState("State 1")
    object FinalState : DefaultFinalUnitState("State 2")
}

fun main() {
    val machine = createStateMachine {
        logger = StateMachine.Logger { println(it) }

        addInitialState(States1.InitialState(42)) {
            transitionOn<SwitchEvent1> {
                guard = { this@addInitialState.value > 10 }
                targetState = { States1.FinalState }
            }
        }

        addFinalState(States1.FinalState)
    }

    machine.processEvent(SwitchEvent1)
}