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
import io.kotest.matchers.types.shouldBeSameInstanceAs
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.transition.Transition

class AccessorsApiTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "requireTransition()" {
                val state = object : DefaultState() {}

                lateinit var firstTransition: Transition<*>
                lateinit var secondTransition: Transition<*>

                createTestStateMachine(coroutineStarterType) {
                    addInitialState(state) {
                        firstTransition = transition<FirstEvent>("first transition")
                        secondTransition = transition<SecondEvent>()
                    }
                }

                state.requireTransition("first transition") shouldBeSameInstanceAs firstTransition
                state.requireTransition<SecondEvent>() shouldBeSameInstanceAs secondTransition
                shouldThrowWithMessage<IllegalArgumentException>(
                    "Transition some transition not found"
                ) { state.requireTransition("some transition") }
                shouldThrowWithMessage<IllegalArgumentException>(
                    "Transition for SwitchEvent not found"
                ) { state.requireTransition<SwitchEvent>() }
            }

            "requireState()" {
                lateinit var first: State
                lateinit var second: State
                val machine = createTestStateMachine(coroutineStarterType) {
                    first = initialState("first")
                    second = state("second")
                }

                machine.requireState("first") shouldBeSameInstanceAs first
                machine.requireState("second", recursive = false) shouldBeSameInstanceAs second
                shouldThrowWithMessage<IllegalArgumentException>(
                    "State third not found"
                ) { machine.requireState("third") }
            }

            "requireState() recursive" {
                lateinit var first: State
                lateinit var firstNested: State
                val machine = createTestStateMachine(coroutineStarterType) {
                    first = initialState("first") {
                        firstNested = initialState("firstNested")
                    }
                }

                machine.requireState("firstNested") shouldBeSameInstanceAs firstNested
                shouldThrowWithMessage<IllegalArgumentException>(
                    "State firstNested not found"
                ) { machine.requireState("firstNested", recursive = false) shouldBeSameInstanceAs firstNested }
                first.requireState("firstNested", recursive = false) shouldBeSameInstanceAs firstNested
            }

            "requireState() by type" {
                open class SubclassState : DefaultState()
                open class UnusedSubclassState : DefaultState()

                class FirstState(val value: Int = 42) : DefaultState()
                class SecondState : SubclassState()
                class ThirdInnerState : SubclassState()

                lateinit var first: FirstState
                lateinit var second: State
                lateinit var third: State
                val machine = createTestStateMachine(coroutineStarterType) {
                    first = addInitialState(FirstState())
                    second = addState(SecondState()) {
                        third = addState(ThirdInnerState())
                    }
                }

                machine.requireState<FirstState>() shouldBeSameInstanceAs first
                machine.requireState<FirstState>().value shouldBe first.value
                machine.requireState<SecondState>(recursive = false) shouldBeSameInstanceAs second

                shouldThrowWithMessage<IllegalArgumentException>(
                    "State ThirdInnerState not found"
                ) { machine.requireState<ThirdInnerState>(recursive = false) }
                machine.requireState<ThirdInnerState>() shouldBeSameInstanceAs third

                shouldThrowWithMessage<IllegalArgumentException>(
                    "State UnusedSubclassState not found"
                ) { machine.requireState<UnusedSubclassState>() }
                shouldThrowWithMessage<IllegalArgumentException>(
                    "More than one state matches State"
                ) { machine.requireState<State>() }
                shouldThrowWithMessage<IllegalArgumentException>(
                    "More than one state matches SubclassState"
                ) { machine.requireState<SubclassState>() }
            }

            "requireState() of nested machine not allowed" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState {
                        addInitialState(createTestStateMachine(coroutineStarterType) {
                            initialState("nested")
                            transition<SwitchEvent>("transition")
                        })
                    }
                }

                shouldThrowWithMessage<IllegalArgumentException>(
                    "State nested not found"
                ) { machine.requireState("nested") }
            }
        }
    }
})