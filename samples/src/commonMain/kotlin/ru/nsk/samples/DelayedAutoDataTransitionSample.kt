/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2026.
 * All rights reserved.
 */

package ru.nsk.samples

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.state.DataState
import ru.nsk.kstatemachine.state.autoDataTransition
import ru.nsk.kstatemachine.state.dataState
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.onEntry
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine
import kotlin.time.Duration.Companion.milliseconds

/**
 * Type-safe variant of [autoDataTransition] with `delay` — the producer is invoked once when the
 * timer fires, and its value lands in the target [DataState].
 *
 * Requires `kstatemachine-coroutines`.
 */
fun main() = runBlocking {
    lateinit var timedOut: DataState<String>

    val machine = createStateMachine(this, name = "DelayedAutoDataTransitionSample") {
        logger = StateMachine.Logger { println(it()) }

        timedOut = dataState<String>("timedOut") {
            onEntry { println("Timeout reason: $data") }
        }
        initialState("waiting") {
            autoDataTransition<String> {
                delay = 50.milliseconds
                targetState = timedOut
                dataProducer = { "no user response within 50ms" }
            }
        }
    }

    delay(100.milliseconds)
    check(timedOut.isActive)
    check(timedOut.data == "no user response within 50ms")
    println("Done")
}
