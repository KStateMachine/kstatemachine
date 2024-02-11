package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.verify
import io.mockk.verifySequence
import ru.nsk.kstatemachine.ProcessingResult.IGNORED

class IgnoredEventHandlerTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
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