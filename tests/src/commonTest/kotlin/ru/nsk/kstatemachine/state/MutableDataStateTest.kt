/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import ru.nsk.kstatemachine.CoroutineStarterType
import ru.nsk.kstatemachine.SwitchEvent
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.event.DataEvent
import ru.nsk.kstatemachine.state.MutableDataStateTestData.IntEvent

private object MutableDataStateTestData {
    class IntEvent(override val data: Int) : DataEvent<Int>
}

class MutableDataStateTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "MutableDataState behaviour" {
                lateinit var mutableDataState: MutableDataState<Int>
                val machine = createTestStateMachine(coroutineStarterType) {
                    val state1 = initialState {
                        dataTransitionOn<IntEvent, Int> { targetState = { mutableDataState } }
                    }
                    mutableDataState = mutableDataState<Int>("mutableDataState") {
                        onEntry { data shouldBe 42 }
                        transition<SwitchEvent> { targetState = state1 }
                    }
                }
                machine.processEvent(IntEvent(42))
                mutableDataState.isActive shouldBe true
                mutableDataState.lastData shouldBe 42
                mutableDataState.data shouldBe 42

                mutableDataState.setData(1)
                mutableDataState.lastData shouldBe 1
                mutableDataState.data shouldBe 1

                machine.processEvent(SwitchEvent)
                mutableDataState.isActive shouldBe false
                mutableDataState.lastData shouldBe 1
                shouldThrowWithMessage<IllegalStateException>(
                    "Data is not set. Is DefaultMutableDataState(mutableDataState) state active?"
                ) { mutableDataState.data }

                mutableDataState.setData(2)
                mutableDataState.lastData shouldBe 2
                shouldThrowWithMessage<IllegalStateException>(
                    "Data is not set. Is DefaultMutableDataState(mutableDataState) state active?"
                ) { mutableDataState.data }
            }

            "Set MutableDataState data on substate entry" {
                lateinit var mutableState1: MutableDataState<Int>
                lateinit var state12: State
                val machine = createTestStateMachine(coroutineStarterType) {
                    mutableState1 = initialMutableDataState<Int>(defaultData = 1) {
                        initialState()
                        state12 = state {
                            onEntry { this@initialMutableDataState.setData(2) }
                        }
                    }
                    transition<SwitchEvent> { targetState = state12 }
                }
                machine.processEvent(SwitchEvent)
                mutableState1.lastData shouldBe 2
            }

            "mutableDataState" {
                lateinit var mutableDataState: MutableDataState<Int>
                createTestStateMachine(coroutineStarterType) {
                    initialChoiceDataState { mutableDataState }
                    mutableDataState = mutableDataState<Int>(defaultData = 0)
                }
                mutableDataState.data shouldBe 0
            }

            "mutableDataState scoped" {
                lateinit var mutableDataState: MutableDataState<Int>
                createTestStateMachine(coroutineStarterType) {
                    initialChoiceDataState { mutableDataState }
                    mutableDataState = mutableDataState<Int>(defaultData = 0) {
                        onEntry { setData(1) }
                    }
                }
                mutableDataState.data shouldBe 1
            }

            "initialMutableDataState" {
                lateinit var mutableDataState: MutableDataState<Int>
                createTestStateMachine(coroutineStarterType) {
                    mutableDataState = initialMutableDataState<Int>(defaultData = 1)
                }
                mutableDataState.data shouldBe 1
            }

            "initialMutableDataState scoped" {
                lateinit var mutableDataState: MutableDataState<Int>
                createTestStateMachine(coroutineStarterType) {
                    mutableDataState = initialMutableDataState<Int>(defaultData = 1) {
                        onEntry { setData(0) }
                    }
                }
                mutableDataState.data shouldBe 0
            }

            "initialFinalMutableDataState" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialFinalMutableDataState<Int>(defaultData = 1)
                }
                machine.isFinished shouldBe true
            }

            "initialFinalMutableDataState scoped" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialFinalMutableDataState<Int>(defaultData = 1) {
                        onEntry { setData(0) }
                    }
                }
                machine.isFinished shouldBe true
            }

            "finalMutableDataState" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    val dataState = finalMutableDataState<Int>(defaultData = 1)
                    initialState {
                        dataTransition<IntEvent, Int> { targetState = dataState }
                    }
                }
                machine.processEvent(IntEvent(0))
                machine.isFinished shouldBe true
            }

            "finalMutableDataState scoped" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    val dataState = finalMutableDataState<Int>(defaultData = 1) {}
                    initialState {
                        dataTransition<IntEvent, Int> { targetState = dataState }
                    }
                }
                machine.processEvent(IntEvent(0))
                machine.isFinished shouldBe true
            }
        }
    }
})