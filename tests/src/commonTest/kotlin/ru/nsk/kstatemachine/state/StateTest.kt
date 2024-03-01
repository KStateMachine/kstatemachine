package ru.nsk.kstatemachine.state

import io.kotest.assertions.throwables.shouldThrowUnitWithMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ru.nsk.kstatemachine.CoroutineStarterType
import ru.nsk.kstatemachine.SwitchEvent
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.metainfo.UmlMetaInfo
import ru.nsk.kstatemachine.state.StateTestData.SubclassState
import ru.nsk.kstatemachine.statemachine.destroy
import ru.nsk.kstatemachine.statemachine.processEventBlocking
import ru.nsk.kstatemachine.transition.onTriggered

private object StateTestData {
    class SubclassState : DefaultState() {
        val dataField = 0
    }
}

class StateTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
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

        "metaInfo cannot be changed on running machine" {
            lateinit var state: State
            createTestStateMachine(coroutineStarterType) {
                state = initialState {
                    metaInfo = UmlMetaInfo("label")
                }
            }
            shouldThrowUnitWithMessage<IllegalStateException>("Can not change metaInfo after state machine started") {
                state.metaInfo = UmlMetaInfo("fail label")
            }
        }

        "set state payload" {
            lateinit var state: State
            val machine = createTestStateMachine(coroutineStarterType) {
                state = initialState {
                    payload = "arbitrary data"
                }
            }
            state.payload shouldBe "arbitrary data"
            machine.destroy()
            state.payload shouldBe null
        }

        /** This code should not compile */
        "dsl marker" {
//            createTestStateMachine(coroutineStarterType) {
//                val subclassState = addState(SubclassState())
//                onStarted {}
//                subclassState {
//                    // forbidden
//                    onStarted {}
//                    // forbidden
//                    addState(SubclassState())
//                    transition<SwitchEvent> {
//
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
//                    onTransitionTriggered {}
//                }
//                onTransitionTriggered {}
//                setInitialState(subclassState)
//            }
        }
    }
})