package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ru.nsk.kstatemachine.StateTestData.SubclassState

private object StateTestData {
    class SubclassState : DefaultState() {
        val dataField = 0
    }
}

class StateTest : StringSpec({
    CoroutineStarterType.values().forEach { coroutineStarterType ->
        "state subclass" {
            val machine = createTestStateMachine(coroutineStarterType) {
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

            machine.processEventBlocking(SwitchEvent)
            machine.processEventBlocking(SwitchEvent)
        }

        "final state transition with explicit state" {
            val machine = createTestStateMachine(coroutineStarterType) {
                val final = addFinalState(DefaultFinalState("final")) {
                    transition<SwitchEvent>() // does nothing in this case
                }
                setInitialState(final)
            }
            machine.isFinished shouldBe true
        }

        "explicit final state marker usage" {
            class MyState : DefaultState(), FinalState

            val machine = createTestStateMachine(coroutineStarterType) {
                val final = addFinalState(MyState())
                setInitialState(final)
            }
            machine.isFinished shouldBe true
        }

        /** This code should not compile */
        "dsl marker" {
//            createTestStateMachine(coroutineStarterType) {
//                val subclassState = addState(SubclassState())
//
//                subclassState {
//                    // forbidden
//                    addState(SubclassState())
//                    transition<SwitchEvent> {
//                        // forbidden
//                        onEntry {
//                            if (dataField == 0)
//                                println("we can read data from state")
//                        }
//                        onTriggered {}
//                        // forbidden
//                        transition<SwitchEvent> {}
//                    }
//                    setInitialState(subclassState)
//                    // forbidden
//                    onTransition { _, _, _, _ -> }
//                }
//                onTransition { _, _, _, _ -> }
//                setInitialState(subclassState)
//            }
        }
    }
})