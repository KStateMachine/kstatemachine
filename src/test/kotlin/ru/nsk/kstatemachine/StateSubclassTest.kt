package ru.nsk.kstatemachine

import org.junit.jupiter.api.Test

class CustomState : State("subclass") {
    val dataField = 0
}

private object SomeEvent1 : Event

class StateSubclassTest {
    @Test
    fun stateSubclass() {
        val stateMachine = createStateMachine {
            // simple but little bit explicit, easy to forget addState() call
            val subclassState = addState(CustomState())

            val simpleState = state("first") {
                transition<SomeEvent1> {
                    targetState = subclassState
                    onTriggered { }
                }
            }

            subclassState {
                transition<SomeEvent1> {
                    targetState = simpleState
                    onTriggered { }
                    //TODO forbid this
                    onEntry {
                        if (dataField == 0)
                            log("we can read data from state")
                        //TODO forbid this
                        onEntry {
                            if (dataField == 0)
                                log("we can read data from state")
                        }
                    }
                    onExit {
                        if (dataField == 0)
                            log("we can read data from state")
                    }
                }
                onEntry {
                    if (dataField == 0)
                        log("we can read data from state")
                }
                onExit {
                    if (dataField == 0)
                        log("we can read data from state")
                }
            }

            setInitialState(simpleState)
        }

        stateMachine.processEvent(SomeEvent1)
        stateMachine.processEvent(SomeEvent1)
    }
}