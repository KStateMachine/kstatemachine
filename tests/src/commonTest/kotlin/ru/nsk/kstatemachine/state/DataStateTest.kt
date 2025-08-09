/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import ru.nsk.kstatemachine.CoroutineStarterType
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.event.DataEvent
import ru.nsk.kstatemachine.state.DataStateTestData.IntEvent

private object DataStateTestData {
    class IntEvent(override val data: Int) : DataEvent<Int>
}

class DataStateTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "dataState" {
                lateinit var dataState: DataState<Int>
                createTestStateMachine(coroutineStarterType) {
                    initialChoiceDataState { dataState }
                    dataState = dataState<Int>(defaultData = 0)
                }
                dataState.data shouldBe 0
            }

            "dataState scoped" {
                lateinit var dataState: DataState<Int>
                createTestStateMachine(coroutineStarterType) {
                    initialChoiceDataState { dataState }
                    dataState = dataState<Int>(defaultData = 0) {}
                }
                dataState.data shouldBe 0
            }

            "initialDataState" {
                lateinit var dataState: DataState<Int>
                createTestStateMachine(coroutineStarterType) {
                    dataState = initialDataState<Int>(defaultData = 1)
                }
                dataState.data shouldBe 1
            }

            "initialDataState scoped" {
                lateinit var dataState: DataState<Int>
                createTestStateMachine(coroutineStarterType) {
                    dataState = initialDataState<Int>(defaultData = 1) {}
                }
                dataState.data shouldBe 1
            }

            "initialFinalDataState" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialFinalDataState<Int>(defaultData = 1)
                }
                machine.isFinished shouldBe true
            }

            "initialFinalDataState scoped" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialFinalDataState<Int>(defaultData = 1) {}
                }
                machine.isFinished shouldBe true
            }

            "finalDataState" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    val dataState = finalDataState<Int>(defaultData = 1)
                    initialState {
                        dataTransition<IntEvent, Int> { targetState = dataState }
                    }
                }
                machine.processEvent(IntEvent(0))
                machine.isFinished shouldBe true
            }

            "finalDataState scoped" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    val dataState = finalDataState<Int>(defaultData = 1) {}
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