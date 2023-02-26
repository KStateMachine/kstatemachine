package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.verify
import io.mockk.verifySequence
import ru.nsk.kstatemachine.ProcessingResult.IGNORED

class IgnoredEventHandlerTest : StringSpec({
    "ignored event handler" {
        val callbacks = mockkCallbacks()

        val machine = createTestStateMachine {
            initialState("first")

            ignoredEventHandler = StateMachine.IgnoredEventHandler { _, _ ->
                callbacks.onIgnoredEvent(SwitchEvent)
            }
        }

        machine.processEvent(SwitchEvent) shouldBe IGNORED
        verifySequence { callbacks.onIgnoredEvent(SwitchEvent) }
    }

    "exceptional ignored event handler" {
        val machine = createTestStateMachine {
            initialState("first")

            ignoredEventHandler = StateMachine.IgnoredEventHandler { event, _ ->
                testError("unexpected $event")
            }
        }

        shouldThrow<TestException> { machine.processEvent(SwitchEvent) }
        machine.isDestroyed shouldBe false
    }

    "process event on finished state machine" {
        val callbacks = mockkCallbacks()

        val machine = createTestStateMachine {
            setInitialState(finalState("final"))

            onFinished { callbacks.onFinished(this) }

            ignoredEventHandler = StateMachine.IgnoredEventHandler { event, _ ->
                callbacks.onIgnoredEvent(event)
            }
        }

        verifySequenceAndClear(callbacks) { callbacks.onFinished(machine) }

        machine.processEvent(SwitchEvent) shouldBe IGNORED
        verifySequence { callbacks.onIgnoredEvent(SwitchEvent) }
    }

    "ignored event on conditional noTransition()" {
        val callbacks = mockkCallbacks()

        val machine = createTestStateMachine {
            initialState {
                transitionConditionally<SwitchEvent> { direction = { noTransition() } }
            }

            ignoredEventHandler = StateMachine.IgnoredEventHandler { event, _ ->
                callbacks.onIgnoredEvent(event)
            }
        }

        machine.processEvent(SwitchEvent) shouldBe IGNORED
        verify { callbacks.onIgnoredEvent(SwitchEvent) }
    }
})