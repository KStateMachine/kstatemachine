package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

class SubclassState : DefaultState() {
    val dataField = 0
}

class StateTest {
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
                    onTriggered { println("Data ${this@subclassState.dataField}") }
                }
            }
        }

        stateMachine.processEvent(SwitchEvent)
        stateMachine.processEvent(SwitchEvent)
    }

    @Test
    fun guardedTransition() {
        val callbacks = mock<Callbacks>()

        var value = "value1";

        val stateMachine = createStateMachine {
            val second = state("second")

            initialState("first") {
                transitionGuarded<SwitchEvent> {
                    guard = { value == "value2" }
                    targetState = second
                    onTriggered {
                        callbacks.onTriggeringEvent(it.event)
                    }
                }
            }
        }

        stateMachine.processEvent(SwitchEvent)
        then(callbacks).shouldHaveZeroInteractions()
        value = "value2"
        stateMachine.processEvent(SwitchEvent)
        then(callbacks).should().onTriggeringEvent(SwitchEvent)
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