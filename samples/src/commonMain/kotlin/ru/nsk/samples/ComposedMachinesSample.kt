/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.samples

import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.event.FinishedEvent
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.samples.ComposedMachinesSample.SwitchEvent

private object ComposedMachinesSample {
    object SwitchEvent : Event
}

/**
 * A [StateMachine] implements [State], so it can be embedded as a child state inside a parent machine.
 * The inner machine starts automatically when the outer machine enters it.
 * When the inner machine finishes, a [FinishedEvent] is generated, which the outer machine can handle.
 */
fun main() = runBlocking {
    val innerMachine = createStateMachine(this, "inner", start = false) {
        logger = StateMachine.Logger { println("inner: ${it()}") }

        val innerFinal = finalState("innerFinal")
        initialState("innerState1") {
            transition<SwitchEvent> { targetState = innerFinal }
        }
    }

    val outerFinal: IState
    val outerMachine = createStateMachine(this, "outer") {
        logger = StateMachine.Logger { println("outer: ${it()}") }

        outerFinal = finalState("outerFinal")

        // inner machine is added as a child state; it starts automatically on entry
        addInitialState(innerMachine) {
            // FinishedEvent is emitted when the inner machine reaches its final state
            transition<FinishedEvent> { targetState = outerFinal }
        }
    }

    check(innerMachine.isRunning)
    check(!outerMachine.isFinished)

    outerMachine.processEvent(SwitchEvent)

    check(innerMachine.isFinished)
    check(outerMachine.isFinished)
    println("Both machines finished: outer=${outerMachine.isFinished}, inner=${innerMachine.isFinished}")
}
