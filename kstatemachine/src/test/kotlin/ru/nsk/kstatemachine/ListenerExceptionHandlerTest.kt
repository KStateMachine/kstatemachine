package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verifySequence
import ru.nsk.kstatemachine.Testing.startFrom

class ListenerExceptionHandlerTest : StringSpec({
    "default ListenerExceptionHandler rethrows exception from state onEntry() on start() call" {
        shouldThrow<TestException> {
            createStateMachine {
                logger = StateMachine.Logger { println(it) }

                initialState {
                    onEntry { testError("test exception") }
                }
            }
        }
    }

    "default ListenerExceptionHandler rethrows exception from state onEntry() on manual start() call" {
        val machine = createStateMachine(start = false) {
            initialState {
                onEntry { testError("test exception") }
            }
        }

        shouldThrow<TestException> { machine.start() }
        machine.isDestroyed shouldBe false
    }

    "default ListenerExceptionHandler rethrows exception from machine onEntry() on start() call" {
        val callbacks = mockkCallbacks()
        lateinit var state1: State

        val machine = createStateMachine(start = false) {
            onStarted { callbacks.onStarted(this) }
            callbacks.listen(this)
            onEntry { testError("test exception") }

            state1 = initialState {
                callbacks.listen(this)
            }
        }
        shouldThrow<TestException> { machine.start() }

        verifySequence {
            callbacks.onStarted(machine)
            callbacks.onEntryState(machine)
            callbacks.onEntryState(state1)
        }
    }

    "default ListenerExceptionHandler rethrows exception from onStarted() on start() call" {
        shouldThrow<TestException> {
            createStateMachine {
                onStarted { testError("test exception") }

                initialState()
            }
        }
    }

    "default ListenerExceptionHandler rethrows exception from startFrom()" {
        lateinit var state2: State
        val machine = createStateMachine(start = false) {
            initialState()
            state2 = state {
                onEntry { testError("test exception") }
            }
        }

        shouldThrow<TestException> { machine.startFrom(state2) }
        machine.isDestroyed shouldBe false
    }

    "default ListenerExceptionHandler rethrows exception from stop()" {
        val machine = createStateMachine {
            initialState()
            onStopped { testError("test exception") }
        }

        shouldThrow<TestException> { machine.stop() }
        machine.isDestroyed shouldBe false
    }

    "silent ListenerExceptionHandler is called" {
        val handlerMock = mockk<StateMachine.ListenerExceptionHandler>(relaxed = true)

        val machine = createStateMachine {
            listenerExceptionHandler = handlerMock

            initialState {
                onEntry { testError("test exception") }
            }
        }

        verifySequence { handlerMock.onException(ofType<TestException>()) }
        machine.isDestroyed shouldBe false
    }

    "machine is destroyed on unrecoverable exception and ListenerExceptionHandler is not called" {
        val handlerMock = mockk<StateMachine.ListenerExceptionHandler>(relaxed = true)

        val machine = createStateMachine {
            logger = StateMachine.Logger { println(it) }

            listenerExceptionHandler = handlerMock

            val state2 = state()
            initialState {
                transition<SwitchEvent> {
                    guard = { testError("test exception") }
                    targetState = state2
                }
            }
        }

        shouldThrow<TestException> {
            machine.processEvent(SwitchEvent)
        }

        machine.isDestroyed shouldBe true
        verifySequence(inverse = true) { handlerMock.onException(ofType<TestException>()) }
    }
})