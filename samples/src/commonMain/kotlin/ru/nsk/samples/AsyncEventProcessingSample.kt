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
import ru.nsk.kstatemachine.statemachine.ProcessingResult
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.statemachine.processEventByAsync
import ru.nsk.kstatemachine.statemachine.processEventByLaunch

private object AsyncEventProcessingSample {
    object SwitchEvent : Event
}

/**
 * [processEventByAsync] dispatches an event without suspending and returns a [kotlinx.coroutines.Deferred]
 * whose value is the [ProcessingResult] once the event has been processed.
 *
 * [processEventByLaunch] dispatches an event as fire-and-forget: no return value, no waiting.
 *
 * Both require a machine created with a [kotlinx.coroutines.CoroutineScope] (i.e. [createStateMachine]).
 */
fun main() = runBlocking {
    // Demo 1: processEventByAsync — non-suspending dispatch with an awaitable result
    val asyncMachine = createStateMachine(this, "asyncMachine") {
        logger = StateMachine.Logger { println(it()) }
        val state2 = finalState("state2")
        initialState("state1") {
            transition<AsyncEventProcessingSample.SwitchEvent> { targetState = state2 }
        }
    }

    val deferred = asyncMachine.processEventByAsync(AsyncEventProcessingSample.SwitchEvent)
    println("Event dispatched via processEventByAsync — result not yet available")
    val result = deferred.await()
    check(result == ProcessingResult.PROCESSED)
    println("processEventByAsync result: $result")

    // Demo 2: processEventByLaunch — fire-and-forget, no return value
    val launchMachine = createStateMachine(this, "launchMachine") {
        logger = StateMachine.Logger { println(it()) }
        val state2 = finalState("state2") {
            onEntry { println("state2 entered — event was processed") }
        }
        initialState("state1") {
            transition<AsyncEventProcessingSample.SwitchEvent> { targetState = state2 }
        }
    }

    launchMachine.processEventByLaunch(AsyncEventProcessingSample.SwitchEvent)
    println("Event dispatched via processEventByLaunch — continuing without waiting")
    // runBlocking will wait for the launched coroutine to complete before returning
}
