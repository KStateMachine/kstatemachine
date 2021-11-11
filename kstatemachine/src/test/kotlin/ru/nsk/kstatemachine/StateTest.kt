package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class SubclassState : DefaultState() {
    val dataField = 0
}

class StateTest : StringSpec({
    "state subclass" {
        val machine = createStateMachine {
            // simple but little bit explicit, easy to forget addState() call
            val subclassState = addState(SubclassState()) {
                onEntry { println("Enter state with data: ${this@addState.dataField}") }
            }

            val simpleState = initialState {
                transition<SwitchEvent> { targetState = subclassState }
            }

            subclassState {
                transition<SwitchEvent> {
                    targetState = simpleState
                    onTriggered { println("Data ${this@subclassState.dataField}") }
                }
            }
        }

        machine.processEvent(SwitchEvent)
        machine.processEvent(SwitchEvent)
    }

    "final state transition" {
        createStateMachine {
            val final = finalState("final") {
                shouldThrow<UnsupportedOperationException> { transition<SwitchEvent>() }
            }
            setInitialState(final)
        }
    }

    "final state transition with explicit state" {
        createStateMachine {
            val final = addFinalState(DefaultFinalState("final")) {
                shouldThrow<UnsupportedOperationException> { transition<SwitchEvent>() }
            }
            setInitialState(final)
        }
    }

    "explicit final state marker usage" {
        class MyState : DefaultState(), FinalState {
            override fun <E : Event> addTransition(transition: Transition<E>) =
                super<FinalState>.addTransition(transition)
        }

        createStateMachine {
            val final = addFinalState(MyState()) {
                shouldThrow<UnsupportedOperationException> { transition<SwitchEvent>() }
            }
            setInitialState(final)
        }
    }

    "requireState()" {
        lateinit var first: State
        lateinit var second: State
        val machine = createStateMachine {
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
        val machine = createStateMachine {
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
        val machine = createStateMachine {
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

    "activeStates()" {
        lateinit var state1: State
        lateinit var state2: State
        lateinit var state21: State
        lateinit var state211: State

        val machine = createStateMachine {
            state1 = initialState("state1") {
                transitionOn<SwitchEvent> {
                    targetState = { state2 }
                }
            }
            state2 = state("state2") {
                state21 = initialState("state21") {
                    state211 = addInitialState(createStateMachine(start = false) {
                        // should not be included
                        initialState("state2111")
                    })
                }
            }
        }

        machine.activeStates(true) should containExactly(machine, state1)
        machine.activeStates() should containExactly(state1)

        machine.processEvent(SwitchEvent)

        machine.activeStates(true) should containExactly(machine, state2, state21, state211)
        machine.activeStates() should containExactly(state2, state21, state211)

        state2.activeStates(true) should containExactly(state2, state21, state211)
        state2.activeStates() should containExactly(state21, state211)
    }

    "activeStates() in parallel child mode" {
        lateinit var state1: State
        lateinit var state2: State

        val machine = createStateMachine(childMode = ChildMode.PARALLEL) {
            state1 = state()
            state2 = state()
        }

        machine.activeStates(true ) should containExactly(machine, state1, state2)
        machine.activeStates() should containExactly(state1, state2)
    }

    // This code should not compile
    "dsl marker" {
//        createStateMachine {
//            val subclassState = addState(SubclassState())
//
//            subclassState {
//                // forbidden
//                addState(SubclassState())
//                transition<SwitchEvent> {
//                    // forbidden
//                    onEntry {
//                        if (dataField == 0)
//                            println("we can read data from state")
//                    }
//                    onTriggered {}
//                    // forbidden
//                    transition<SwitchEvent> {}
//                }
//                setInitialState(subclassState)
//                // forbidden
//                onTransition { _, _, _, _ -> }
//            }
//            onTransition { _, _, _, _ -> }
//            setInitialState(subclassState)
//        }
    }
})