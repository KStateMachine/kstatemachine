package ru.nsk.kstatemachine

import org.junit.jupiter.api.Test

class SubclassState : State() {
    val dataField = 0
}

class StateSubclassTest {
    @Test
    fun stateSubclass() {
        val stateMachine = createStateMachine {
            // simple but little bit explicit, easy to forget addState() call
            val subclassState = addState(SubclassState())

            val simpleState = initialState {
                transition<SwitchEvent> {
                    targetState = subclassState
                    onTriggered { }
                }
            }

            subclassState {
                transition<SwitchEvent> {
                    targetState = simpleState
                    onTriggered { }
                }
            }
        }

        stateMachine.processEvent(SwitchEvent)
        stateMachine.processEvent(SwitchEvent)
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