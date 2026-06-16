/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2026.
 * All rights reserved.
 */

package ru.nsk.samples

import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.state.DataState
import ru.nsk.kstatemachine.state.autoDataTransition
import ru.nsk.kstatemachine.state.dataState
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.onEntry
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.samples.AutoDataTransitionSample.LoginResult

private object AutoDataTransitionSample {
    data class LoginResult(val userId: String, val sessionToken: String)
}

/**
 * Type-safe auto transition variant [autoDataTransition] — the carried payload lands directly in the target
 * [DataState], with no [DataEvent] subclass to declare.
 */
fun main() = runBlocking {
    lateinit var session: DataState<LoginResult>

    val machine = createStateMachine(this, name = "AutoDataTransitionSample") {
        logger = StateMachine.Logger { println(it()) }

        session = dataState<LoginResult>("session") {
            onEntry { println("Logged in as: $data") }
        }
        initialState("authenticating") {
            // Producer runs once at entry, exactly when the eventless transition fires.
            autoDataTransition(targetState = session) {
                LoginResult(userId = "u-42", sessionToken = "abc123")
            }
        }
    }

    check(session.isActive)
    check(session.data == LoginResult("u-42", "abc123"))
    println("Done")
}
