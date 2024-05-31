/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.transition

import io.kotest.core.spec.style.StringSpec
import io.mockk.called
import io.mockk.verify
import io.mockk.verifySequence
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.processEventBlocking

class GuardedTransitionTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "guarded transition" {
            val callbacks = mockkCallbacks()

            var value = "value1"

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it()) }

                val second = state("second")

                initialState("first") {
                    transition<SwitchEvent> {
                        guard = {
                            this@initialState.log { "$event $argument" }
                            value == "value2"
                        }
                        targetState = second
                        callbacks.listen(this)
                    }
                }
            }

            machine.processEventBlocking(SwitchEvent)
            verify { callbacks wasNot called }

            value = "value2"
            machine.processEventBlocking(SwitchEvent)
            verify { callbacks.onTransitionTriggered(SwitchEvent) }
        }

        "guarded transitionOn() with lateinit state" {
            val callbacks = mockkCallbacks()

            var value = "value1"

            val machine = createTestStateMachine(coroutineStarterType) {
                lateinit var second: State

                initialState("first") {
                    transitionOn<SwitchEvent> {
                        guard = { value == "value2" }
                        targetState = { second }
                        callbacks.listen(this)
                    }
                }

                second = state("second")
            }

            machine.processEventBlocking(SwitchEvent)
            verify { callbacks wasNot called }

            value = "value2"
            machine.processEventBlocking(SwitchEvent)
            verify { callbacks.onTransitionTriggered(SwitchEvent) }
        }

        "guarded transition same event" {
            val callbacks = mockkCallbacks()

            lateinit var state1: State
            lateinit var state2: State
            lateinit var state3: State

            val machine = createTestStateMachine(coroutineStarterType) {
                state1 = initialState("state1") {
                    callbacks.listen(this)

                    transitionOn<SwitchEvent> {
                        guard = { false }
                        targetState = { state2 }
                        callbacks.listen(this)
                    }

                    transitionOn<SwitchEvent> {
                        guard = { true }
                        targetState = { state3 }
                        callbacks.listen(this)
                    }
                }

                state2 = state("state2") { callbacks.listen(this) }
                state3 = state("state3") { callbacks.listen(this) }
            }

            verifySequenceAndClear(callbacks) { callbacks.onStateEntry(state1) }

            machine.processEventBlocking(SwitchEvent)

            verifySequence {
                callbacks.onTransitionTriggered(SwitchEvent)
                callbacks.onStateExit(state1)
                callbacks.onStateEntry(state3)
            }
        }
    }
})