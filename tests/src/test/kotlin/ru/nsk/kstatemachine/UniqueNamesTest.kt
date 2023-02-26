package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec

class UniqueNamesTest : StringSpec({
    "do not allow transitions with same name" {
        shouldThrow<IllegalStateException> {
            createTestStateMachine {
                initialState {
                    transition<SwitchEvent>("transition")
                    transition<SwitchEvent>("transition")
                }
            }
        }
    }

    "do not allow nested states with same name" {
        shouldThrow<IllegalStateException> {
            createTestStateMachine("first") {
                initialState("first")
            }
        }
    }

    "do not allow same names in machine as atomic state" {
        shouldThrow<IllegalStateException> {
            createTestStateMachine("first") {
                initialState {
                    addInitialState(createTestStateMachine("first") {
                        initialState()
                    })
                }
            }
        }
    }

    "allow same names in nested machines" {
        createTestStateMachine {
            initialState("first") {
                addInitialState(createTestStateMachine {
                    initialState("first")
                })
            }
        }
    }
})