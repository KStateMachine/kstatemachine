package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec

class SubclassState : DefaultState() {
    val dataField = 0
}

class StateTest : StringSpec({
    "state subclass" {
        val machine = createStateMachine {
            // simple but little explicit, easy to forget addState() call
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