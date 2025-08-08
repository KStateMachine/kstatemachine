/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.statemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.testing.Testing.startFromBlocking

class ListenerExceptionHandlerTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "default ListenerExceptionHandler rethrows exception from state onEntry() on start() call" {
            shouldThrow<TestException> {
                createTestStateMachine(coroutineStarterType) {
                    logger = StateMachine.Logger { println(it()) }

                    initialState {
                        onEntry { testError() }
                    }
                }
            }
        }

        "default ListenerExceptionHandler rethrows exception from state onEntry() on manual start() call" {
            val machine = createTestStateMachine(coroutineStarterType, start = false) {
                initialState {
                    onEntry { testError() }
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
                onEntry { testError() }

                state1 = initialState {
                    callbacks.listen(this)
                }
            }
            shouldThrow<TestException> { machine.startBlocking() }

            verifySequence {
                callbacks.onStarted(machine)
                callbacks.onStateEntry(machine)
                callbacks.onStateEntry(state1)
            }
        }

        "default ListenerExceptionHandler rethrows exception from onStarted() on start() call" {
            shouldThrow<TestException> {
                createTestStateMachine(coroutineStarterType) {
                    onStarted { testError() }

                    initialState()
                }
            }
        }

        "default ListenerExceptionHandler rethrows exception from startFrom()" {
            lateinit var state2: State
            val machine = createTestStateMachine(coroutineStarterType, start = false) {
                initialState()
                state2 = state {
                    onEntry { testError() }
                }
            }

            shouldThrow<TestException> { machine.startFromBlocking(state2) }
            machine.isDestroyed shouldBe false
        }

        "default ListenerExceptionHandler rethrows exception from stop()" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState()
                onStopped { testError() }
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
                    onEntry { testError() }
                }
            }

            coVerifySequence { handlerMock.onException(ofType<TestException>()) }
            machine.isDestroyed shouldBe false
        }

        "machine is destroyed on unrecoverable exception and ListenerExceptionHandler is not called" {
            val handlerMock = mockk<StateMachine.ListenerExceptionHandler>(relaxed = true)

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it()) }

                listenerExceptionHandler = handlerMock

                val state2 = state()
                initialState {
                    transition<SwitchEvent> {
                        guard = { testError() }
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