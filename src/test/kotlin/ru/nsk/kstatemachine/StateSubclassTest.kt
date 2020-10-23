package ru.nsk.kstatemachine

import org.junit.jupiter.api.Test

class SubclassState : State("subclass") {
    val dataField = 0
}

class StateSubclassTest {
    @Test
    fun stateSubclass() {
        val stateMachine = createStateMachine {
            // simple but little bit explicit, easy to forget addState() call
            val subclassState = addState(SubclassState())

            val simpleState = state("first") {
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

            setInitialState(simpleState)
        }

        stateMachine.processEvent(SwitchEvent)
        stateMachine.processEvent(SwitchEvent)
    }


    @Test
    fun FIXME() {
        createStateMachine {
            val subclassState = addState(SubclassState())

            subclassState {
                transition<SwitchEvent> {
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
            }
            setInitialState(subclassState)
        }

    }
}