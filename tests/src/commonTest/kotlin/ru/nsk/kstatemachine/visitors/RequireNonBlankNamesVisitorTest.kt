/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.visitors

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import ru.nsk.kstatemachine.CoroutineStarterType
import ru.nsk.kstatemachine.SwitchEvent
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.statemachine.buildCreationArguments

class RequireNonBlankNamesVisitorTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "check machine with multiple blank names" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState {
                        transition<SwitchEvent>()
                    }
                }

                machine.hasBlankNames() shouldBe true
                shouldThrow<IllegalStateException> {
                    machine.checkNonBlankNames()
                }.message shouldStartWith "There were blank names in states"
            }

            "check machine blank name" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState("initial") {
                        transition<SwitchEvent>("transition")
                    }
                }

                machine.hasBlankNames() shouldBe true
                shouldThrow<IllegalStateException> {
                    machine.checkNonBlankNames()
                }.message shouldStartWith "There were blank names in states"
            }

            "check state blank name" {
                val machine = createTestStateMachine(coroutineStarterType, "machine") {
                    initialState {
                        transition<SwitchEvent>("transition")
                    }
                }

                machine.hasBlankNames() shouldBe true
                shouldThrow<IllegalStateException> {
                    machine.checkNonBlankNames()
                }.message shouldStartWith "There were blank names in states"
            }

            "check transition blank name" {
                val machine = createTestStateMachine(coroutineStarterType, "machine") {
                    initialState("initial") {
                        transition<SwitchEvent>()
                    }
                }

                machine.hasBlankNames() shouldBe true
                shouldThrow<IllegalStateException> {
                    machine.checkNonBlankNames()
                }.message shouldStartWith "There were blank names in states"
            }

            "check machine without blank names" {
                val machine = createTestStateMachine(coroutineStarterType, "machine") {
                    initialState("initial") {
                        transition<SwitchEvent>("transition")
                    }
                }

                machine.hasBlankNames() shouldBe false
                machine.checkNonBlankNames()
            }

            "check machine started with blank names and disabled check" {
                createTestStateMachine(
                    coroutineStarterType,
                    creationArguments = buildCreationArguments { requireNonBlankNames = false }
                ) {
                    initialState()
                }
            }

            "check exception thrown on machine start with blank names and enabled check" {
                val machine = createTestStateMachine(
                    coroutineStarterType,
                    start = false,
                    creationArguments = buildCreationArguments { requireNonBlankNames = true }
                ) {
                    initialState()
                }
                shouldThrow<IllegalStateException> {
                    machine.start()
                }.message shouldStartWith "There were blank names in states"
            }
        }
    }
})
