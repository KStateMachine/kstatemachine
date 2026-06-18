/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2026.
 * All rights reserved.
 */

package ru.nsk.samples

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import ru.nsk.kstatemachine.state.DataState
import ru.nsk.kstatemachine.state.autoDataTransition
import ru.nsk.kstatemachine.state.dataState
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.onEntry
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Type-safe variant of [autoDataTransition] with `delay` — the producer is invoked once when the
 * timer fires, and its value lands in the target [DataState].
 *
 * Requires `kstatemachine-coroutines`.
 */
fun main() = runBlocking {
    lateinit var timedOut: DataState<String>
    val timedOutEntered = CompletableDeferred<Unit>()

    createStateMachine(this, name = "DelayedAutoDataTransitionSample") {
        logger = StateMachine.Logger { println(it()) }

        timedOut = dataState<String>("timedOut") {
            onEntry {
                println("Timeout reason: $data")
                timedOutEntered.complete(Unit)
            }
        }
        initialState("waiting") {
            autoDataTransition {
                delay = 50.milliseconds
                targetState = timedOut
                dataProducer = { "no user response within 50ms" }
            }
        }
    }

    withTimeout(5.seconds) { timedOutEntered.await() }
    check(timedOut.isActive)
    check(timedOut.data == "no user response within 50ms")
    println("Done")
}
