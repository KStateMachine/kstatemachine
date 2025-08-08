/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.statemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.mockk.verify
import io.mockk.verifySequence
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.state.initialFinalState
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.onFinished
import ru.nsk.kstatemachine.state.transitionConditionally
import ru.nsk.kstatemachine.statemachine.ProcessingResult.IGNORED
import ru.nsk.kstatemachine.transition.noTransition

class IgnoredEventHandlerTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "throwing ignored event handler" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState("first")

                    ignoredEventHandler = throwingIgnoredEventHandler()
                }

                shouldThrow<IllegalStateException> {
                    machine.processEvent(SwitchEvent)
                }.message shouldEndWith "The machine was configured with throwingIgnoredEventHandler, that forbids such behaviour."
            }

            "ignored event handler" {
                val callbacks = mockkCallbacks()

                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState("first")

                    ignoredEventHandler = StateMachine.IgnoredEventHandler {
                        callbacks.onIgnoredEvent(SwitchEvent)
                    }
                }

                machine.processEventBlocking(SwitchEvent) shouldBe IGNORED
                verifySequence { callbacks.onIgnoredEvent(SwitchEvent) }
            }

            "exceptional ignored event handler" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState("first")

                    ignoredEventHandler = StateMachine.IgnoredEventHandler {
                        testError("unexpected ${it.event::class.simpleName}")
                    }
                }

                shouldThrowWithMessage<TestException>(
                    "unexpected SwitchEvent"
                ) { machine.processEventBlocking(SwitchEvent) }
                machine.isDestroyed shouldBe false
            }

            "process event on finished state machine" {
                val callbacks = mockkCallbacks()

                val machine = createTestStateMachine(coroutineStarterType) {
                    initialFinalState("final")

                    onFinished { callbacks.onStateFinished(this) }

                    ignoredEventHandler = StateMachine.IgnoredEventHandler {
                        callbacks.onIgnoredEvent(it.event)
                    }
                }

                verifySequenceAndClear(callbacks) { callbacks.onStateFinished(machine) }

                machine.processEventBlocking(SwitchEvent) shouldBe IGNORED
                verifySequence { callbacks.onIgnoredEvent(SwitchEvent) }
            }

            "ignored event on conditional noTransition()" {
                val callbacks = mockkCallbacks()

                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState {
                        transitionConditionally<SwitchEvent> { direction = { noTransition() } }
                    }

                    ignoredEventHandler = StateMachine.IgnoredEventHandler {
                        callbacks.onIgnoredEvent(it.event)
                    }
                }

                machine.processEventBlocking(SwitchEvent) shouldBe IGNORED
                verify { callbacks.onIgnoredEvent(SwitchEvent) }
            }
        }
    }
})