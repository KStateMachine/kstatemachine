/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.samples

import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.transition.*
import ru.nsk.samples.TargetlessTransitionSample.IncrementEvent
import ru.nsk.samples.TargetlessTransitionSample.SwitchEvent

private object TargetlessTransitionSample {
    object IncrementEvent : Event
    object SwitchEvent : Event
}

/**
 * A target-less (internal) transition has no [targetState].
 * It triggers actions but does NOT cause exit/entry of the current state —
 * the state machine stays exactly where it is.
 * This is useful for in-place reactions to events, such as updating a counter.
 */
fun main() = runBlocking {
    var counter = 0
    var entryCount = 0

    val machine = createStateMachine(this) {
        logger = StateMachine.Logger { println(it()) }

        val finalState = finalState("done")

        initialState("counterState") {
            onEntry { entryCount++ }

            // target-less: no state change, just executes the action
            transition<IncrementEvent> {
                onTriggered { counter++ }
            }

            transition<SwitchEvent> { targetState = finalState }
        }
    }

    check(entryCount == 1)

    machine.processEvent(IncrementEvent)
    machine.processEvent(IncrementEvent)
    machine.processEvent(IncrementEvent)

    // state was NOT re-entered; only the action ran
    check(entryCount == 1)
    check(counter == 3)
    println("Counter: $counter, re-entries: ${entryCount - 1}")

    machine.processEvent(SwitchEvent)
    check(entryCount == 1) // still 1, the final state entry does not count for counterState
}
