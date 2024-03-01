package ru.nsk.samples

import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.samples.GuardedTransitionSample.States.State1
import ru.nsk.samples.GuardedTransitionSample.States.State2
import ru.nsk.samples.GuardedTransitionSample.SwitchEvent

private object GuardedTransitionSample {
    object SwitchEvent : Event

    sealed class States : DefaultState() {
        class State1(val value: Int) : States()
        object State2 : States(), FinalState
    }
}

fun main() = runBlocking {
    val machine = createStateMachine(this) {
        logger = StateMachine.Logger { println(it()) }

        addInitialState(State1(42)) {
            transition<SwitchEvent> {
                guard = { this@addInitialState.value > 10 }
                targetState = State2
            }
        }

        addFinalState(State2)
    }

    machine.processEvent(SwitchEvent)

    check(State2 in machine.activeStates())
}