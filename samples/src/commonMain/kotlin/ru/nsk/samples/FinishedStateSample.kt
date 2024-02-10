package ru.nsk.samples

import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.*
import ru.nsk.samples.FinishedStateSample.SwitchEvent

private object FinishedStateSample {
    object SwitchEvent : Event
}

/**
 * States with child mode [ChildMode.EXCLUSIVE] are finished when enter their [FinalState] child.
 * States with child mode [ChildMode.PARALLEL] are finished when all their direct children are finished
 */
fun main() = runBlocking {
    lateinit var state1: IState
    lateinit var state11: IState
    lateinit var state12: IState
    lateinit var state2: IState

    val machine = createStateMachine(this, childMode = ChildMode.PARALLEL) {
        state1 = state("State1", childMode = ChildMode.PARALLEL) {
            state11 = state("State11") {
                addInitialState(DefaultFinalState("Final state111"))
            }
            state12 = state("State12") {
                addInitialState(DefaultFinalState("Final state121"))
            }
        }
        state2 = state("State2") {
            val finalState22 = finalState("State22")
            initialState {
                transition<SwitchEvent> { targetState = finalState22 }
            }
        }
        onFinished { println("$this finished") }
    }

    machine.processEvent(SwitchEvent)

    check(state1.isFinished)
    check(state11.isFinished)
    check(state12.isFinished)
    check(state2.isFinished)
    check(machine.isFinished)
}