package ru.nsk.samples

import ru.nsk.kstatemachine.*
import ru.nsk.samples.UndoTransitionSample.SwitchEvent

private object UndoTransitionSample {
    object SwitchEvent : Event
}

/**
 * Undo transitions with [StateMachine.undoBlocking] method or [UndoEvent]
 */
fun main() {
    lateinit var state1: State
    lateinit var state2: State
    lateinit var state3: State

    val machine = createStateMachine(enableUndo = true) {
        state1 = initialState("state1") {
            transitionOn<SwitchEvent> { targetState = { state2 } }
        }
        state2 = state("state2") {
            onEntry {
                println("$this entered with event ${it.event}, ${it.unwrappedEvent}")
                check(it.unwrappedEvent is SwitchEvent)
            }

            transitionOn<SwitchEvent> { targetState = { state3 } }
        }
        state3 = state("state3") {
            onEntry { machine.undo() }
        }
    }

    machine.processEventBlocking(SwitchEvent)
    machine.processEventBlocking(SwitchEvent)

    machine.undoBlocking() // same as machine.processEventBlocking(UndoEvent)

    check(state1 in machine.activeStates())
}