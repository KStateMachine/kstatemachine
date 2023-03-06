package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.called
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import ru.nsk.kstatemachine.Testing.startFrom

class ListenerExceptionHandlerTest : StringSpec({
    CoroutineStarterType.values().forEach { coroutineStarterType ->
        "default ListenerExceptionHandler rethrows exception from state onEntry() on start() call" {
            shouldThrow<TestException> {
                createTestStateMachine(coroutineStarterType) {
                    logger = StateMachine.Logger { println(it) }

                    initialState {
                        onEntry { testError("test exception") }
                    }
                }
            }
        }

        "default ListenerExceptionHandler rethrows exception from state onEntry() on manual start() call" {
            val machine = createTestStateMachine(coroutineStarterType, start = false) {
                initialState {
                    onEntry { testError("test exception") }
                }
            }

            shouldThrow<TestException> { machine.startBlocking() }
            machine.isDestroyed shouldBe false
        }

        "default ListenerExceptionHandler rethrows exception from machine onEntry() on start() call" {
            val callbacks = mockkCallbacks()
            lateinit var state1: State

            val machine = createTestStateMachine(coroutineStarterType, start = false) {
                onStarted { callbacks.onStarted(this) }
                callbacks.listen(this)
                onEntry { testError("test exception") }

                state1 = initialState {
                    callbacks.listen(this)
                }
            }
            shouldThrow<TestException> { machine.startBlocking() }

            verifySequence {
                callbacks.onStarted(machine)
                callbacks.onEntryState(machine)
                callbacks.onEntryState(state1)
            }
        }

        "default ListenerExceptionHandler rethrows exception from onStarted() on start() call" {
            shouldThrow<TestException> {
                createTestStateMachine(coroutineStarterType) {
                    onStarted { testError("test exception") }

                    initialState()
                }
            }
        }

        "default ListenerExceptionHandler rethrows exception from startFrom()" {
            lateinit var state2: State
            val machine = createTestStateMachine(coroutineStarterType, start = false) {
                initialState()
                state2 = state {
                    onEntry { testError("test exception") }
                }
            }

            shouldThrow<TestException> { machine.startFrom(state2) }
            machine.isDestroyed shouldBe false
        }

        "default ListenerExceptionHandler rethrows exception from stop()" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState()
                onStopped { testError("test exception") }
            }

            shouldThrow<TestException> { machine.stopBlocking() }
            machine.stopBlocking() // does nothing
            machine.isDestroyed shouldBe false
        }

        "silent ListenerExceptionHandler is called" {
            val handlerMock = mockk<StateMachine.ListenerExceptionHandler>(relaxed = true)

            val machine = createTestStateMachine(coroutineStarterType) {
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

            val machine = createTestStateMachine(coroutineStarterType) {
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
                machine.processEventBlocking(SwitchEvent)
            }

            machine.isDestroyed shouldBe true
            verify { handlerMock wasNot called }
        }
    }
})