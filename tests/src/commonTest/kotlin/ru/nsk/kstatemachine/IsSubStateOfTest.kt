/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.state.addInitialState
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.state

class IsSubStateOfTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "isSubStateOf()" {
                lateinit var state1: IState
                lateinit var state12: IState
                lateinit var state2: IState
                val machine = createTestStateMachine(coroutineStarterType) {
                    state1 = initialState {
                        state12 = initialState()
                    }
                    state2 = state()
                }
                state1.isSubStateOf(state1) shouldBe false
                machine.isSubStateOf(machine) shouldBe false

                state1.isSubStateOf(machine) shouldBe true
                state12.isSubStateOf(machine) shouldBe true
                state12.isSubStateOf(state1) shouldBe true
                state2.isSubStateOf(state1) shouldBe false
                machine.isSubStateOf(state1) shouldBe false
                state1.isSubStateOf(state12) shouldBe false
            }

            "isSubStateOf() composed machines" {
                lateinit var innerState1: IState
                val inner = createTestStateMachine(coroutineStarterType) {
                    innerState1 = initialState()
                }
                val outer = createTestStateMachine(coroutineStarterType) {
                    addInitialState(inner)
                }
                inner.isSubStateOf(outer) shouldBe true
                innerState1.isSubStateOf(inner) shouldBe true
                innerState1.isSubStateOf(outer) shouldBe false
            }
        }
    }
})