/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.samples

import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.event.UndoEvent
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.buildCreationArguments
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.statemachine.undo
import ru.nsk.kstatemachine.transition.unwrappedEvent
import ru.nsk.samples.UndoTransitionSample.SwitchEvent

private object UndoTransitionSample {
    object SwitchEvent : Event
}

/**
 * Undo transitions with [StateMachine.undo] method or [UndoEvent]
 */
fun main() = runBlocking {
    val state1: State
    lateinit var state2: State
    lateinit var state3: State

    val machine = createStateMachine(
        this,
        creationArguments = buildCreationArguments { isUndoEnabled = true }
    ) {
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

    machine.processEvent(SwitchEvent)
    machine.processEvent(SwitchEvent)

    machine.undo() // same as machine.processEvent(UndoEvent)

    check(state1 in machine.activeStates())
}