/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.transition

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
import ru.nsk.kstatemachine.state.autoDataTransition
import ru.nsk.kstatemachine.state.autoDataTransitionOn
import ru.nsk.kstatemachine.state.autoTransition
import ru.nsk.kstatemachine.state.autoTransitionConditionally
import ru.nsk.kstatemachine.state.autoTransitionOn
import ru.nsk.kstatemachine.state.dataState
import ru.nsk.kstatemachine.state.initialDataState
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
class DelayedAutoTransitionTest : FreeSpec({
    "autoTransition(delay) throws for StdLib machine" {
        // The error fires during machine start when onEntry triggers scheduleAfterDelay.
        shouldThrowWithMessage<IllegalStateException>(
            "scheduleAfterDelay requires a machine created with coroutines support (createStateMachine)"
        ) {
            createStdLibStateMachine {
                val target = state("target")
                initialState("source") {
                    autoTransition(delay = FAST, targetState = target)
                }
            }
        }
    }

    "autoTransition(delay) fires after the configured delay" {
        lateinit var target: State

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            createStateMachine(scope) {
                target = state("target")
                initialState("source") {
                    autoTransition(delay = FAST, targetState = target)
                }
            }
            target.isActive.shouldBeFalse()
            delay(WAIT_PAST_FAST)
            target.isActive.shouldBeTrue()
        } finally {
            scope.cancel()
        }
    }

    "autoTransition(delay) does not fire if the source state is exited before the delay elapses" {
        lateinit var source: State
        lateinit var unrelatedTarget: State
        lateinit var delayedTarget: State

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val machine = createStateMachine(scope) {
                delayedTarget = state("delayedTarget")
                unrelatedTarget = state("unrelatedTarget")
                source = initialState("source") {
                    autoTransition(delay = INFINITE, targetState = delayedTarget)
                    transitionOn<SwitchEvent> { targetState = { unrelatedTarget } }
                }
            }
            machine.processEvent(SwitchEvent)
            unrelatedTarget.isActive.shouldBeTrue()

            delay(50.milliseconds)
            delayedTarget.isActive.shouldBeFalse()
        } finally {
            scope.cancel()
        }
    }

    "autoTransition(delay) cancelled on machine stop" {
        lateinit var target: State

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val machine = createStateMachine(scope) {
                target = state("target")
                initialState("source") {
                    autoTransition(delay = INFINITE, targetState = target)
                }
            }
            machine.stop()

            delay(50.milliseconds)
            target.isActive.shouldBeFalse()
        } finally {
            scope.cancel()
        }
    }

    "autoTransition(delay) cancelled on machine destroy" {
        lateinit var target: State

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val machine = createStateMachine(scope) {
                target = state("target")
                initialState("source") {
                    autoTransition(delay = INFINITE, targetState = target)
                }
            }
            machine.destroy()

            delay(50.milliseconds)
            target.isActive.shouldBeFalse()
        } finally {
            scope.cancel()
        }
    }

    "autoTransition scoped with delay — guard rejection keeps state put and does not auto-restart" {
        lateinit var source: State
        lateinit var target: State
        var fireCount = 0

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            createStateMachine(scope) {
                target = state("target")
                source = initialState("source") {
                    autoTransition {
                        delay = FAST
                        targetState = target
                        guard = {
                            fireCount++
                            false
                        }
                    }
                }
            }
            delay(WAIT_PAST_FAST)
            source.isActive.shouldBeTrue()
            target.isActive.shouldBeFalse()
            fireCount shouldBe 1

            delay(WAIT_PAST_FAST)
            fireCount shouldBe 1
        } finally {
            scope.cancel()
        }
    }

    "autoDataTransition(delay) carries producer value into DataState" {
        lateinit var produced: DataState<Int>
        var producerCalls = 0

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            createStateMachine(scope) {
                produced = dataState<Int>("produced")
                initialState("source") {
                    autoDataTransition(delay = FAST, targetState = produced) {
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

    "autoTransitionOn scoped with delay resolves the target lazily" {
        lateinit var target: State

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            createStateMachine(scope) {
                initialState("source") {
                    autoTransitionOn {
                        delay = FAST
                        targetState = { target }
                    }
                }
                target = state("target")
            }
            delay(WAIT_PAST_FAST)
            target.isActive.shouldBeTrue()
        } finally {
            scope.cancel()
        }
    }

    "autoTransitionConditionally scoped with delay fires the direction lambda at fire time" {
        lateinit var even: State
        lateinit var odd: State
        var counter = 0

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            createStateMachine(scope) {
                even = state("even")
                odd = state("odd")
                initialState("source") {
                    autoTransitionConditionally {
                        delay = FAST
                        direction = {
                            if (counter % 2 == 0) targetState(even) else targetState(odd)
                        }
                    }
                }
            }
            delay(WAIT_PAST_FAST)
            even.isActive.shouldBeTrue()
            odd.isActive.shouldBeFalse()
        } finally {
            scope.cancel()
        }
    }

    "autoDataTransition scoped with delay carries producer value" {
        lateinit var produced: DataState<String>

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            createStateMachine(scope) {
                produced = dataState<String>("produced")
                initialState("source") {
                    autoDataTransition<String> {
                        delay = FAST
                        targetState = produced
                        dataProducer = { "hello" }
                    }
                }
            }
            delay(WAIT_PAST_FAST)
            produced.isActive.shouldBeTrue()
            produced.data shouldBe "hello"
        } finally {
            scope.cancel()
        }
    }

    "autoDataTransitionOn scoped with delay resolves the data target lazily" {
        lateinit var produced: DataState<Int>

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            createStateMachine(scope) {
                initialState("source") {
                    autoDataTransitionOn {
                        delay = FAST
                        targetState = { produced }
                        dataProducer = { 99 }
                    }
                }
                produced = dataState<Int>("produced")
            }
            delay(WAIT_PAST_FAST)
            produced.isActive.shouldBeTrue()
            produced.data shouldBe 99
        } finally {
            scope.cancel()
        }
    }

    "self-targeted autoDataTransition with delay refreshes DataState data" {
        lateinit var counter: DataState<Int>
        var producerCalls = 0

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            createStateMachine(scope) {
                counter = initialDataState<Int>("counter", defaultData = 0) {
                    autoDataTransition(delay = FAST) {
                        producerCalls++
                        1
                    }
                }
            }
            delay(WAIT_PAST_FAST)
            producerCalls shouldBe 1
        } finally {
            scope.cancel()
        }
    }

    "missing delay in scoped builder fires immediately (delay defaults to null)" {
        lateinit var target: State

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            createStateMachine(scope) {
                target = state("target")
                initialState("source") {
                    autoTransition {
                        // delay not set — should fire immediately on entry
                        targetState = target
                    }
                }
            }
            // no delay() call needed — fires synchronously on entry
            target.isActive.shouldBeTrue()
        } finally {
            scope.cancel()
        }
    }
})
