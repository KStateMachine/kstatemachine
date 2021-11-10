package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.should
import org.junit.jupiter.api.Test

class SubclassState : DefaultState() {
    val dataField = 0
}

class StateTest : StringSpec({
    @Test
    fun stateSubclass() {
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

    @Test
    fun finalStateTransition() {
        createStateMachine {
            val final = finalState("final") {
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

    "final state transition explicit state" {
        createStateMachine {
            val final = addFinalState(DefaultFinalState("final")) {
                shouldThrow<UnsupportedOperationException> { transition<SwitchEvent>() }
            }
            setInitialState(final)
        }
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