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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withTimeout
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
import ru.nsk.kstatemachine.state.onEntry
import ru.nsk.kstatemachine.state.state
import ru.nsk.kstatemachine.state.transitionOn
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.statemachine.createStdLibStateMachine
import ru.nsk.kstatemachine.statemachine.destroy
import ru.nsk.kstatemachine.statemachine.stop
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val FAST = 30.milliseconds
private val INFINITE = Long.MAX_VALUE.milliseconds
private val TEST_TIMEOUT = 5.seconds

class DelayedAutoTransitionTest : FreeSpec({
    "autoTransition(delay) throws for StdLib machine" {
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
        val targetEntered = CompletableDeferred<Unit>()
        lateinit var target: State

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            createStateMachine(scope) {
                target = state("target") {
                    onEntry { targetEntered.complete(Unit) }
                }
                initialState("source") {
                    autoTransition(delay = FAST, targetState = target)
                }
            }
            withTimeout(TEST_TIMEOUT) { targetEntered.await() }
            target.isActive.shouldBeTrue()
        } finally {
            scope.cancel()
        }
    }

    "autoTransition(delay) does not fire if the source state is exited before the delay elapses" {
        lateinit var unrelatedTarget: State
        lateinit var delayedTarget: State

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val machine = createStateMachine(scope) {
                delayedTarget = state("delayedTarget")
                unrelatedTarget = state("unrelatedTarget")
                initialState("source") {
                    autoTransition(delay = INFINITE, targetState = delayedTarget)
                    transitionOn<SwitchEvent> { targetState = { unrelatedTarget } }
                }
            }
            // onExit cancels the timer synchronously, so checks are immediate
            machine.processEvent(SwitchEvent)
            unrelatedTarget.isActive.shouldBeTrue()
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
            // onStopped cancels the timer synchronously
            machine.stop()
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
            // onDestroyed cancels the timer synchronously
            machine.destroy()
            target.isActive.shouldBeFalse()
        } finally {
            scope.cancel()
        }
    }

    "autoTransition scoped with delay — guard rejection keeps state put and does not auto-restart" {
        lateinit var source: State
        lateinit var target: State
        var fireCount = 0
        val guardEvaluated = CompletableDeferred<Unit>()

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
                            guardEvaluated.complete(Unit)
                            false
                        }
                    }
                }
            }
            withTimeout(TEST_TIMEOUT) { guardEvaluated.await() }
            source.isActive.shouldBeTrue()
            target.isActive.shouldBeFalse()
            // timer is one-shot and does not restart on guard rejection
            fireCount shouldBe 1
        } finally {
            scope.cancel()
        }
    }

    "autoDataTransition(delay) carries producer value into DataState" {
        lateinit var produced: DataState<Int>
        var producerCalls = 0
        val targetEntered = CompletableDeferred<Unit>()

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            createStateMachine(scope) {
                produced = dataState<Int>("produced") {
                    onEntry { targetEntered.complete(Unit) }
                }
                initialState("source") {
                    autoDataTransition {
                        delay = FAST
                        targetState = produced
                        dataProducer = {
                            producerCalls++
                            7
                        }
                    }
                }
            }
            withTimeout(TEST_TIMEOUT) { targetEntered.await() }
            produced.isActive.shouldBeTrue()
            produced.data shouldBe 7
            producerCalls shouldBe 1
        } finally {
            scope.cancel()
        }
    }

    "autoTransitionOn scoped with delay resolves the target lazily" {
        lateinit var target: State
        val targetEntered = CompletableDeferred<Unit>()

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            createStateMachine(scope) {
                initialState("source") {
                    autoTransitionOn {
                        delay = FAST
                        targetState = { target }
                    }
                }
                target = state("target") {
                    onEntry { targetEntered.complete(Unit) }
                }
            }
            withTimeout(TEST_TIMEOUT) { targetEntered.await() }
            target.isActive.shouldBeTrue()
        } finally {
            scope.cancel()
        }
    }

    "autoTransitionConditionally scoped with delay fires the direction lambda at fire time" {
        lateinit var even: State
        lateinit var odd: State
        val evenEntered = CompletableDeferred<Unit>()

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            createStateMachine(scope) {
                even = state("even") {
                    onEntry { evenEntered.complete(Unit) }
                }
                odd = state("odd")
                initialState("source") {
                    autoTransitionConditionally {
                        delay = FAST
                        direction = { targetState(even) }
                    }
                }
            }
            withTimeout(TEST_TIMEOUT) { evenEntered.await() }
            even.isActive.shouldBeTrue()
            odd.isActive.shouldBeFalse()
        } finally {
            scope.cancel()
        }
    }

    "autoDataTransition scoped with delay carries producer value" {
        lateinit var produced: DataState<String>
        val targetEntered = CompletableDeferred<Unit>()

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            createStateMachine(scope) {
                produced = dataState<String>("produced") {
                    onEntry { targetEntered.complete(Unit) }
                }
                initialState("source") {
                    autoDataTransition<String> {
                        delay = FAST
                        targetState = produced
                        dataProducer = { "hello" }
                    }
                }
            }
            withTimeout(TEST_TIMEOUT) { targetEntered.await() }
            produced.isActive.shouldBeTrue()
            produced.data shouldBe "hello"
        } finally {
            scope.cancel()
        }
    }

    "autoDataTransitionOn scoped with delay resolves the data target lazily" {
        lateinit var produced: DataState<Int>
        val targetEntered = CompletableDeferred<Unit>()

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
                produced = dataState<Int>("produced") {
                    onEntry { targetEntered.complete(Unit) }
                }
            }
            withTimeout(TEST_TIMEOUT) { targetEntered.await() }
            produced.isActive.shouldBeTrue()
            produced.data shouldBe 99
        } finally {
            scope.cancel()
        }
    }

    "self-targeted autoDataTransition with delay refreshes DataState data" {
        lateinit var counter: DataState<Int>
        var producerCalls = 0
        val producerCalled = CompletableDeferred<Unit>()

        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            createStateMachine(scope) {
                counter = initialDataState<Int>("counter", defaultData = 0) {
                    autoDataTransitionOn {
                        delay = FAST
                        targetState = { counter }
                        dataProducer = {
                            producerCalls++
                            producerCalled.complete(Unit)
                            1
                        }
                    }
                }
            }
            withTimeout(TEST_TIMEOUT) { producerCalled.await() }
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
                        targetState = target
                    }
                }
            }
            target.isActive.shouldBeTrue()
        } finally {
            scope.cancel()
        }
    }
})
