package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class AccessorsApiTest : StringSpec({
    CoroutineStarterType.values().forEach { coroutineStarterType ->
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
            shouldThrow<IllegalArgumentException> { state.requireTransition("some transition") }
            shouldThrow<IllegalArgumentException> { state.requireTransition<SwitchEvent>() }
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
            shouldThrow<IllegalArgumentException> { machine.requireState("third") }
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
            shouldThrow<IllegalArgumentException> {
                machine.requireState("firstNested", recursive = false) shouldBeSameInstanceAs firstNested
            }
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

            shouldThrow<IllegalArgumentException> { machine.requireState<ThirdInnerState>(recursive = false) }
            machine.requireState<ThirdInnerState>() shouldBeSameInstanceAs third

            shouldThrow<IllegalArgumentException> { machine.requireState<UnusedSubclassState>() }
            shouldThrow<IllegalArgumentException> { machine.requireState<State>() }
            shouldThrow<IllegalArgumentException> { machine.requireState<SubclassState>() }
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

            shouldThrow<IllegalArgumentException> { machine.requireState("nested") }
        }
    }
})