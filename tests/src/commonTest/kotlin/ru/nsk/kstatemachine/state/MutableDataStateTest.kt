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
import ru.nsk.kstatemachine.state.MutableDataStateTestData.IdEvent

private object MutableDataStateTestData {
    class IdEvent(override val data: Int) : DataEvent<Int>
}

class MutableDataStateTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "MutableDataState behaviour" {
                lateinit var mutableDataState: MutableDataState<Int>
                val machine = createTestStateMachine(coroutineStarterType) {
                    val state1 = initialState {
                        dataTransitionOn<IdEvent, Int> { targetState = { mutableDataState } }
                    }
                    mutableDataState = mutableDataState<Int>("mutableDataState") {
                        onEntry { data shouldBe 42 }
                        transition<SwitchEvent> { targetState = state1 }
                    }
                }
                machine.processEvent(IdEvent(42))
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
        }
    }
})