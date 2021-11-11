package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec

class UniqueNamesTest : StringSpec({
    "do not allow transitions with same name" {
        shouldThrow<IllegalStateException> {
            createStateMachine {
                initialState {
                    transition<SwitchEvent>("transition")
                    transition<SwitchEvent>("transition")
                }
            }
        }
    }

    "do not allow nested states with same name" {
        shouldThrow<IllegalStateException> {
            createStateMachine("first") {
                initialState("first")
            }
        }
    }

    "do not allow same names in machine as atomic state" {
        shouldThrow<IllegalStateException> {
            createStateMachine("first") {
                initialState {
                    addInitialState(createStateMachine("first") {
                        initialState()
                    })
                }
            }
        }
    }

    "allow same names in nested machines" {
        createStateMachine {
            initialState("first") {
                addInitialState(createStateMachine {
                    initialState("first")
                })
            }
        }
    }
})