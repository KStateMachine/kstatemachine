package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

class IgnoredEventHandlerTest {
    @Test
    fun ignoredEventHandler() {
        val callbacks = mock<Callbacks>()

        val machine = createStateMachine {
            initialState("first")

            ignoredEventHandler = StateMachine.IgnoredEventHandler { _, _ ->
                callbacks.onIgnoredEvent(SwitchEvent)
            }
        }

        machine.processEvent(SwitchEvent)
        then(callbacks).should().onIgnoredEvent(SwitchEvent)
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
        val callbacks = mock<Callbacks>()

        val machine = createStateMachine {
            setInitialState(finalState("final"))

            onFinished { callbacks.onFinished(this) }

            ignoredEventHandler = StateMachine.IgnoredEventHandler { event, _ ->
                callbacks.onIgnoredEvent(event)
            }
        }

        then(callbacks).should().onFinished(machine)

        machine.processEvent(SwitchEvent)
        then(callbacks).should().onIgnoredEvent(SwitchEvent)
    }
}