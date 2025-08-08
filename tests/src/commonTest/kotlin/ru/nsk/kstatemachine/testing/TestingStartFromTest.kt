/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.testing

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.onStarted
import ru.nsk.kstatemachine.testing.Testing.startFrom
import ru.nsk.kstatemachine.testing.Testing.startFromBlocking

enum class ApiType { StateByReference, StateByName }

class TestingStartFromTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            ApiType.entries.forEach { apiType ->
                "startFromBlocking()" {
                    val callbacks = mockkCallbacks()

                    lateinit var state2: State
                    lateinit var state22: State

                    val machine = createTestStateMachine(coroutineStarterType, start = false) {
                        callbacks.listen(this)

                        initialState("state1") { callbacks.listen(this) }
                        state2 = state("state2") {
                            callbacks.listen(this)

                            initialState("state21") { callbacks.listen(this) }
                            state22 = state("state22") { callbacks.listen(this) }
                        }

                        onStarted { callbacks.onStarted(this) }
                    }

                    when (apiType) {
                        ApiType.StateByReference -> machine.startFromBlocking(state22)
                        ApiType.StateByName -> machine.startFromBlocking("state22")
                    }
                    verifySequenceAndClear(callbacks) {
                        callbacks.onStarted(machine)
                        callbacks.onStateEntry(machine)
                        callbacks.onStateEntry(state2)
                        callbacks.onStateEntry(state22)
                    }
                }
            }

            "data startFromBlocking()" {
                val callbacks = mockkCallbacks()
                lateinit var state2: DataState<Int>
                val machine = createTestStateMachine(coroutineStarterType, start = false) {
                    initialState("state1") {
                        callbacks.listen(this)
                    }
                    state2 = dataState("state2", defaultData = 1) {
                        callbacks.listen(this)
                    }
                }
                machine.startFromBlocking(state2, data = 42)
                state2.data shouldBe 42
            }

            "data startFrom()" {
                val callbacks = mockkCallbacks()
                lateinit var state2: DataState<Int>
                val machine = createTestStateMachine(coroutineStarterType, start = false) {
                    initialState("state1") {
                        callbacks.listen(this)
                    }
                    state2 = dataState("state2", defaultData = 1) {
                        callbacks.listen(this)
                    }
                }
                machine.startFrom(state2, data = 42)
                state2.data shouldBe 42
            }

            "startFrom() multiple target states" {
                val callbacks = mockkCallbacks()

                lateinit var state2: State
                lateinit var state21: State
                lateinit var state211: State
                lateinit var state22: State
                lateinit var state222: State
                lateinit var state23: State
                lateinit var state232: State

                val machine = createTestStateMachine(coroutineStarterType, start = false) {
                    callbacks.listen(this)

                    initialState("state1") { callbacks.listen(this) }
                    state2 = state("state2", ChildMode.PARALLEL) {
                        callbacks.listen(this)

                        state21 = state("state21") {
                            callbacks.listen(this)
                            state211 = initialState("state211") { callbacks.listen(this) }
                            state("state212") { callbacks.listen(this) }
                        }
                        state22 = state("state22") {
                            callbacks.listen(this)
                            initialState("state221") { callbacks.listen(this) }
                            state222 = state("state222") { callbacks.listen(this) }
                        }
                        state23 = state("state23") {
                            callbacks.listen(this)
                            initialState("state231") { callbacks.listen(this) }
                            state232 = state("state232") { callbacks.listen(this) }
                        }
                    }

                    onStarted { callbacks.onStarted(this) }
                }

                machine.startFrom(state222, state232)

                verifySequenceAndClear(callbacks) {
                    callbacks.onStarted(machine)
                    callbacks.onStateEntry(machine)
                    callbacks.onStateEntry(state2)
                    callbacks.onStateEntry(state21)
                    callbacks.onStateEntry(state211)
                    callbacks.onStateEntry(state22)
                    callbacks.onStateEntry(state222)
                    callbacks.onStateEntry(state23)
                    callbacks.onStateEntry(state232)
                }
                machine.activeStates()
                    .shouldContainExactly(state2, state21, state211, state22, state222, state23, state232)
            }
        }
    }
})