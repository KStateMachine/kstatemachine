/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.visitors

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import ru.nsk.kstatemachine.CoroutineStarterType
import ru.nsk.kstatemachine.SwitchEvent
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.state.addInitialState
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.transition

class CheckUniqueNamesVisitorTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "do not allow transitions with same name" {
                shouldThrow<IllegalStateException> {
                    createTestStateMachine(coroutineStarterType) {
                        initialState {
                            transition<SwitchEvent>("transition")
                            transition<SwitchEvent>("transition")
                        }
                    }
                }
            }

            "do not allow nested states with same name" {
                shouldThrow<IllegalStateException> {
                    createTestStateMachine(coroutineStarterType, name = "first") {
                        initialState("first")
                    }
                }
            }

            "do not allow same names in machine as atomic state" {
                shouldThrow<IllegalStateException> {
                    createTestStateMachine(coroutineStarterType, name = "first") {
                        initialState {
                            addInitialState(createTestStateMachine(coroutineStarterType, name = "first") {
                                initialState()
                            })
                        }
                    }
                }
            }

            "allow same names in nested machines" {
                createTestStateMachine(coroutineStarterType) {
                    initialState("first") {
                        addInitialState(createTestStateMachine(coroutineStarterType) {
                            initialState("first")
                        })
                    }
                }
            }
        }
    }
})