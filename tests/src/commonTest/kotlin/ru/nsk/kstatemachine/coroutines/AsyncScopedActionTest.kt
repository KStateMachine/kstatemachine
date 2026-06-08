/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.coroutines

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import ru.nsk.kstatemachine.SwitchEvent
import ru.nsk.kstatemachine.state.State
import ru.nsk.kstatemachine.state.asyncScopedAction
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.state
import ru.nsk.kstatemachine.state.transitionOn
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.statemachine.createStdLibStateMachine
import ru.nsk.kstatemachine.statemachine.destroy
import ru.nsk.kstatemachine.statemachine.stop
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class AsyncScopedActionTest : FreeSpec({
    "asyncScopedAction throws for StdLib machine" {
        createStdLibStateMachine {
            initialState {
                shouldThrowWithMessage<IllegalArgumentException>(
                    "asyncScopedAction requires a StateMachine created with coroutines support (createStateMachine)"
                ) { asyncScopedAction { /* never runs */ } }
            }
        }
    }

    "asyncScopedAction starts on state entry and is cancelled on state exit" {
        // Dispatchers.Unconfined: scope.launch runs inline up to first suspension, and job.cancel()
        // triggers cancellation inline — so assertions remain synchronous.
        var actionStarted = false
        var actionCancelled = false
        lateinit var state2: State

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val machine = createStateMachine(scope) {
                initialState("state1") {
                    asyncScopedAction {
                        actionStarted = true
                        try {
                            delay(Long.MAX_VALUE.milliseconds)
                        } finally {
                            actionCancelled = true
                        }
                    }
                    transitionOn<SwitchEvent> { targetState = { state2 } }
                }
                state2 = state("state2")
            }

            actionStarted shouldBe true
            actionCancelled shouldBe false
            machine.processEvent(SwitchEvent)
            actionCancelled shouldBe true
        } finally {
            scope.cancel()
        }
    }

    "asyncScopedAction is relaunched on state re-entry" {
        var launchCount = 0
        lateinit var state1: State
        lateinit var state2: State

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val machine = createStateMachine(scope) {
                state1 = initialState("state1") {
                    asyncScopedAction {
                        launchCount++
                        delay(Long.MAX_VALUE.milliseconds)
                    }
                    transitionOn<SwitchEvent> { targetState = { state2 } }
                }
                state2 = state("state2") {
                    transitionOn<SwitchEvent> { targetState = { state1 } }
                }
            }

            launchCount shouldBe 1
            machine.processEvent(SwitchEvent)  // exit state1, first job cancelled
            machine.processEvent(SwitchEvent)  // re-enter state1, new job launched inline
            launchCount shouldBe 2
        } finally {
            scope.cancel()
        }
    }

    "asyncScopedAction job is cancelled on machine stop" {
        var actionCancelled = false

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val machine = createStateMachine(scope) {
                initialState("state1") {
                    asyncScopedAction {
                        try {
                            delay(Long.MAX_VALUE.milliseconds)
                        } finally {
                            actionCancelled = true
                        }
                    }
                }
            }

            actionCancelled shouldBe false
            machine.stop()
            actionCancelled shouldBe true
        } finally {
            scope.cancel()
        }
    }

    "asyncScopedAction job is cancelled on machine destroy" {
        var actionCancelled = false

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val machine = createStateMachine(scope) {
                initialState("state1") {
                    asyncScopedAction {
                        try {
                            delay(Long.MAX_VALUE.milliseconds)
                        } finally {
                            actionCancelled = true
                        }
                    }
                }
            }

            actionCancelled shouldBe false
            machine.destroy()
            actionCancelled shouldBe true
        } finally {
            scope.cancel()
        }
    }
})