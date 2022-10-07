package ru.nsk.samples

import ru.nsk.kstatemachine.*
import ru.nsk.samples.UndoTransitionSample.SwitchEvent

private object UndoTransitionSample {
    object SwitchEvent : Event
}

/**
 * Undo transitions with [StateMachine.undo] method or [UndoEvent]
 */
fun main() {
    lateinit var state1: State
    lateinit var state2: State

    val machine = createStateMachine(enableUndo = true) {
        state1 = initialState("state1") {
            transitionOn<SwitchEvent> { targetState = { state2 } }
        }
        state2 = state("state2")
    }

    machine.processEvent(SwitchEvent)
    check(state2 in machine.activeStates())

    machine.undo() // same as machine.processEvent(UndoEvent)
    check(state1 in machine.activeStates())
}