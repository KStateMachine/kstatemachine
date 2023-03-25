package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec

class UniqueNamesTest : StringSpec({
    CoroutineStarterType.values().forEach { coroutineStarterType ->
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
})