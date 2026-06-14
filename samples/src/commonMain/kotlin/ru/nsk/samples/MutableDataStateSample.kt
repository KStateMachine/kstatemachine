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
import ru.nsk.samples.MutableDataStateSample.IncrementEvent
import ru.nsk.samples.MutableDataStateSample.ResetEvent
import ru.nsk.samples.MutableDataStateSample.SwitchEvent

private object MutableDataStateSample {
    object IncrementEvent : Event
    object ResetEvent : Event
    object SwitchEvent : Event
}

/**
 * [MutableDataState] extends [DataState] with a [MutableDataState.setData] method that allows
 * updating the state's data directly — without requiring a [ru.nsk.kstatemachine.event.DataEvent].
 * This is useful when state data needs to change in response to target-less internal transitions.
 */
fun main() = runBlocking {
    lateinit var counterState: MutableDataState<Int>

    val machine = createStateMachine(this) {
        logger = StateMachine.Logger { println(it()) }

        val done = finalState("done")

        counterState = initialMutableDataState("counter", defaultData = 0) {
            onEntry { println("Counter entered with value: $data") }

            // target-less: updates data in place without a state change
            transition<IncrementEvent> {
                onTriggered { counterState.setData(counterState.data + 1) }
            }
            transition<ResetEvent> {
                onTriggered { counterState.setData(0) }
            }
            transition<SwitchEvent> { targetState = done }
        }
    }

    check(counterState.data == 0)

    machine.processEvent(IncrementEvent)
    machine.processEvent(IncrementEvent)
    machine.processEvent(IncrementEvent)
    check(counterState.data == 3)
    println("After 3 increments: ${counterState.data}")

    machine.processEvent(ResetEvent)
    check(counterState.data == 0)
    println("After reset: ${counterState.data}")

    machine.processEvent(SwitchEvent)
    // lastData retains the value after the state becomes inactive
    check(counterState.lastData == 0)
}
