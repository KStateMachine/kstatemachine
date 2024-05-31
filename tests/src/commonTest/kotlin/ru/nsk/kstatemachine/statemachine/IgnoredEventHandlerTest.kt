/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.statemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.verify
import io.mockk.verifySequence
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.state.initialFinalState
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.onFinished
import ru.nsk.kstatemachine.state.transitionConditionally
import ru.nsk.kstatemachine.statemachine.ProcessingResult.IGNORED
import ru.nsk.kstatemachine.transition.noTransition

class IgnoredEventHandlerTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "throwing ignored event handler" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState("first")

                ignoredEventHandler = throwingIgnoredEventHandler()
            }

            shouldThrow<IllegalStateException> {
                machine.processEvent(SwitchEvent)
            }
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
                    testError("unexpected ${it.event}")
                }
            }

            shouldThrow<TestException> { machine.processEventBlocking(SwitchEvent) }
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
})