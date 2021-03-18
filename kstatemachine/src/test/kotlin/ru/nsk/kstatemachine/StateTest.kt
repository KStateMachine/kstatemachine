package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

class SubclassState : DefaultUnitState() {
    val dataField = 0
}

class StateTest {
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

    /**
     * This test should not compile
     */
    @Test
    fun dslMarker() {
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
//                // forbidden
//                setInitialState(subclassState)
//                // forbidden
//                onTransition { _, _, _, _ -> }
//            }
//            onTransition { _, _, _, _ -> }
//            setInitialState(subclassState)
//        }
    }
}