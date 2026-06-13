/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.coroutines

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import ru.nsk.kstatemachine.SwitchEvent
import ru.nsk.kstatemachine.state.DataState
import ru.nsk.kstatemachine.state.State
import ru.nsk.kstatemachine.state.dataState
import ru.nsk.kstatemachine.state.delayedDataTransition
import ru.nsk.kstatemachine.state.delayedTransition
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.state
import ru.nsk.kstatemachine.state.transitionOn
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.statemachine.createStdLibStateMachine
import ru.nsk.kstatemachine.statemachine.destroy
import ru.nsk.kstatemachine.statemachine.stop
import kotlin.time.Duration.Companion.milliseconds

private val FAST = 30.milliseconds
private val WAIT_PAST_FAST = 120.milliseconds
private val INFINITE = Long.MAX_VALUE.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class DelayedTransitionTest : FreeSpec({
    "delayedTransition throws for StdLib machine" {
        createStdLibStateMachine {
            initialState("only") {
                shouldThrowWithMessage<IllegalArgumentException>(
                    "delayedTransition requires a StateMachine created with coroutines support (createStateMachine)"
                ) { delayedTransition(delay = FAST, targetState = this) }
            }
        }
    }

    "fires after the configured delay" {
        lateinit var target: State

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val machine = createStateMachine(scope) {
                target = state("target")
                initialState("source") {
                    delayedTransition(delay = FAST, targetState = target)
                }
            }
            target.isActive.shouldBeFalse()
            delay(WAIT_PAST_FAST)
            target.isActive.shouldBeTrue()
        } finally {
            scope.cancel()
        }
    }

    "does not fire if the source state is exited before the delay elapses" {
        lateinit var source: State
        lateinit var unrelatedTarget: State
        lateinit var delayedTarget: State

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val machine = createStateMachine(scope) {
                delayedTarget = state("delayedTarget")
                unrelatedTarget = state("unrelatedTarget")
                source = initialState("source") {
                    delayedTransition(delay = INFINITE, targetState = delayedTarget)
                    transitionOn<SwitchEvent> { targetState = { unrelatedTarget } }
                }
            }
            machine.processEvent(SwitchEvent)
            unrelatedTarget.isActive.shouldBeTrue()

            delay(50.milliseconds) // give any leaked job a chance to fire
            delayedTarget.isActive.shouldBeFalse()
        } finally {
            scope.cancel()
        }
    }

    "cancelled on machine stop" {
        lateinit var target: State

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val machine = createStateMachine(scope) {
                target = state("target")
                initialState("source") {
                    delayedTransition(delay = INFINITE, targetState = target)
                }
            }
            machine.stop()

            delay(50.milliseconds)
            target.isActive.shouldBeFalse()
        } finally {
            scope.cancel()
        }
    }

    "cancelled on machine destroy" {
        lateinit var target: State

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val machine = createStateMachine(scope) {
                target = state("target")
                initialState("source") {
                    delayedTransition(delay = INFINITE, targetState = target)
                }
            }
            machine.destroy()

            delay(50.milliseconds)
            target.isActive.shouldBeFalse()
        } finally {
            scope.cancel()
        }
    }

    "guard rejection at fire time keeps the state put and does not auto-restart" {
        lateinit var source: State
        lateinit var target: State
        var fireCount = 0

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val machine = createStateMachine(scope) {
                target = state("target")
                source = initialState("source") {
                    delayedTransition(delay = FAST, targetState = target, guard = {
                        fireCount++
                        false
                    })
                }
            }
            delay(WAIT_PAST_FAST)
            source.isActive.shouldBeTrue()
            target.isActive.shouldBeFalse()
            fireCount shouldBe 1

            // Wait again — confirm the timer did NOT auto-restart.
            delay(WAIT_PAST_FAST)
            fireCount shouldBe 1
        } finally {
            scope.cancel()
        }
    }

    "delayedDataTransition carries producer value into DataState" {
        lateinit var produced: DataState<Int>
        var producerCalls = 0

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val machine = createStateMachine(scope) {
                produced = dataState<Int>("produced")
                initialState("source") {
                    delayedDataTransition(delay = FAST, targetState = produced) {
                        producerCalls++
                        7
                    }
                }
            }
            delay(WAIT_PAST_FAST)
            produced.isActive.shouldBeTrue()
            produced.data shouldBe 7
            producerCalls shouldBe 1
        } finally {
            scope.cancel()
        }
    }
})
