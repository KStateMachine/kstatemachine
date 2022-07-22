package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verifySequence
import ru.nsk.kstatemachine.Testing.startFrom

class ListenerExceptionHandlerTest : StringSpec({
    "default ListenerExceptionHandler rethrows exception from state onEntry() on start() call" {
        shouldThrow<IllegalStateException> {
            createStateMachine {
                logger = StateMachine.Logger { println(it) }

                initialState {
                    onEntry { error("test exception") }
                }
            }
        }
    }

    "default ListenerExceptionHandler rethrows exception from state onEntry() on manual start() call" {
        val machine = createStateMachine(start = false) {
            initialState {
                onEntry { error("test exception") }
            }
        }

        shouldThrow<IllegalStateException> { machine.start() }
        machine.isDestroyed shouldBe false
    }

    "default ListenerExceptionHandler rethrows exception from machine onEntry() on start() call" {
        val callbacks = mockkCallbacks()
        lateinit var state1: State

        val machine = createStateMachine(start = false) {
            onStarted { callbacks.onStarted(this) }

            onEntry {
                callbacks.listen(this)
                error("test exception")
            }

            state1 = initialState {
                onEntry { callbacks.listen(this) }
            }
        }
        shouldThrow<IllegalStateException> { machine.start() }

        verifySequence {
            callbacks.onStarted(machine)
            callbacks.onEntryState(machine)
            callbacks.onEntryState(state1)
        }
    }

    "default ListenerExceptionHandler rethrows exception from onStarted() on start() call" {
        shouldThrow<IllegalStateException> {
            createStateMachine {
                onStarted { error("test exception") }

                initialState()
            }
        }
    }

    "default ListenerExceptionHandler rethrows exception from startFrom()" {
        lateinit var state2: State
        val machine = createStateMachine(start = false) {
            initialState()
            state2 = state {
                onEntry { error("test exception") }
            }
        }

        shouldThrow<IllegalStateException> { machine.startFrom(state2) }
        machine.isDestroyed shouldBe false
    }

    "default ListenerExceptionHandler rethrows exception from stop()" {
        val machine = createStateMachine {
            initialState()
            onStopped { error("test exception") }
        }

        shouldThrow<IllegalStateException> { machine.stop() }
        machine.isDestroyed shouldBe false
    }

    "silent ListenerExceptionHandler is called" {
        val handlerMock = mockk<StateMachine.ListenerExceptionHandler>(relaxed = true)

        val machine = createStateMachine {
            listenerExceptionHandler = handlerMock

            initialState {
                onEntry { error("test exception") }
            }
        }

        verifySequence { handlerMock.onException(ofType<IllegalStateException>()) }
        machine.isDestroyed shouldBe false
    }

    "machine is destroyed on unrecoverable exception and ListenerExceptionHandler is not called" {

    }
})