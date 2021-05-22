package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.verifySequence
import org.junit.jupiter.api.Test

class IgnoredEventHandlerTest {
    @Test
    fun ignoredEventHandler() {
        val callbacks = mockkCallbacks()

        val machine = createStateMachine {
            initialState("first")

            ignoredEventHandler = StateMachine.IgnoredEventHandler { _, _ ->
                callbacks.onIgnoredEvent(SwitchEvent)
            }
        }

        machine.processEvent(SwitchEvent)
        verifySequence { callbacks.onIgnoredEvent(SwitchEvent) }
    }

    @Test
    fun exceptionalIgnoredEventHandler() {
        val machine = createStateMachine {
            initialState("first")

            ignoredEventHandler = StateMachine.IgnoredEventHandler { event, _ ->
                error("unexpected $event")
            }
        }

        shouldThrow<IllegalStateException> {
            machine.processEvent(SwitchEvent)
        }
    }

    @Test
    fun processEventOnFinishedStateMachine() {
        val callbacks = mockkCallbacks()

        val machine = createStateMachine {
            setInitialState(finalState("final"))

            onFinished { callbacks.onFinished(this) }

            ignoredEventHandler = StateMachine.IgnoredEventHandler { event, _ ->
                callbacks.onIgnoredEvent(event)
            }
        }

        verifySequenceAndClear(callbacks) { callbacks.onFinished(machine) }

        machine.processEvent(SwitchEvent)
        verifySequence { callbacks.onIgnoredEvent(SwitchEvent) }
    }
}