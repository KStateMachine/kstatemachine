/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.transition

import io.kotest.core.spec.style.StringSpec
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.processEventBlocking
import ru.nsk.kstatemachine.transition.TransitionType.EXTERNAL
import ru.nsk.kstatemachine.transition.TransitionType.LOCAL

class ExternalTransitionTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "external transition on machine level" {
            val callbacks = mockkCallbacks()
            lateinit var state1: State
            lateinit var state2: State

            val machine = createTestStateMachine(coroutineStarterType) {
                callbacks.listen(this)
                state1 = initialState("state1") {
                    callbacks.listen(this)
                    transitionOn<SwitchEvent> {
                        targetState = { state2 }
                        type = EXTERNAL
                    }
                }
                state2 = state("state2") {
                    callbacks.listen(this)
                }
            }
            verifySequenceAndClear(callbacks) {
                callbacks.onStateEntry(machine)
                callbacks.onStateEntry(state1)
            }

            machine.processEventBlocking(SwitchEvent)

            verifySequenceAndClear(callbacks) {
                callbacks.onStateExit(state1)
                callbacks.onStateEntry(state2)
            }
        }

        "external transition" {
            val callbacks = mockkCallbacks()
            lateinit var state1: State
            lateinit var state11: State
            lateinit var state12: State

            val machine = createTestStateMachine(coroutineStarterType) {
                callbacks.listen(this)

                state1 = initialState("state1") {
                    callbacks.listen(this)

                    state11 = initialState("state11") {
                        callbacks.listen(this)
                        transitionOn<SwitchEvent> {
                            targetState = { state12 }
                            type = EXTERNAL
                        }
                    }
                    state12 = state("state12") {
                        callbacks.listen(this)
                    }
                }
            }
            verifySequenceAndClear(callbacks) {
                callbacks.onStateEntry(machine)
                callbacks.onStateEntry(state1)
                callbacks.onStateEntry(state11)
            }

            machine.processEventBlocking(SwitchEvent)

            verifySequenceAndClear(callbacks) {
                callbacks.onStateExit(state11)
                callbacks.onStateExit(state1)
                callbacks.onStateEntry(state1)
                callbacks.onStateEntry(state12)
            }
        }

        "external transition self targeted" {
            val callbacks = mockkCallbacks()
            lateinit var state1: State
            lateinit var state11: State

            val machine = createTestStateMachine(coroutineStarterType) {
                callbacks.listen(this)

                state1 = initialState("state1") {
                    callbacks.listen(this)

                    state11 = initialState("state11") {
                        callbacks.listen(this)
                        transitionOn<SwitchEvent> {
                            targetState = { state11 }
                            type = EXTERNAL
                        }
                    }
                    state("state12") {
                        callbacks.listen(this)
                    }
                }
            }
            verifySequenceAndClear(callbacks) {
                callbacks.onStateEntry(machine)
                callbacks.onStateEntry(state1)
                callbacks.onStateEntry(state11)
            }

            machine.processEventBlocking(SwitchEvent)
            verifySequenceAndClear(callbacks) {
                callbacks.onStateExit(state11)
                callbacks.onStateEntry(state11)
            }
        }

        listOf(EXTERNAL, LOCAL).forEach { transitionType ->
            "external target-less transition same as local" {
                val callbacks = mockkCallbacks()
                lateinit var state1: State
                lateinit var state11: State

                val machine = createTestStateMachine(coroutineStarterType) {
                    callbacks.listen(this)

                    state1 = initialState("state1") {
                        callbacks.listen(this)

                        state11 = initialState("state11") {
                            callbacks.listen(this)
                            transition<SwitchEvent> {
                                type = transitionType
                                callbacks.listen(this)
                            }
                        }
                        state("state12") {
                            callbacks.listen(this)
                        }
                    }
                }
                verifySequenceAndClear(callbacks) {
                    callbacks.onStateEntry(machine)
                    callbacks.onStateEntry(state1)
                    callbacks.onStateEntry(state11)
                }

                machine.processEventBlocking(SwitchEvent)
                verifySequenceAndClear(callbacks) {
                    callbacks.onTransitionTriggered(SwitchEvent)
                }
            }
        }
    }
})