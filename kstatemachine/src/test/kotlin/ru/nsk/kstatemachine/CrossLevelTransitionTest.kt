package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import org.junit.jupiter.api.Test

class CrossLevelTransitionTest {
    @Test
    fun parentToChild() {
        val callbacks = mock<Callbacks>()
        val inOrder = inOrder(callbacks)

        lateinit var state1: State
        lateinit var state11: State
        lateinit var state12: State

        val machine = createStateMachine {
            logger = StateMachine.Logger { println(it) }

            state1 = initialState("1") {
                callbacks.listen(this)

                transitionTo<SwitchEvent> {
                    targetState = { state12 }
                    callbacks.listen(this)
                }

                state11 = initialState("11") {
                    callbacks.listen(this)
                }
                state12 = state("12") {
                    callbacks.listen(this)
                }
            }
        }

        machine.processEvent(SwitchEvent)

        then(callbacks).should(inOrder).onEntryState(state1)
        then(callbacks).should(inOrder).onEntryState(state11)
        then(callbacks).should(inOrder).onTriggeredTransition(SwitchEvent)
        then(callbacks).should(inOrder).onExitState(state11)
        then(callbacks).should(inOrder).onEntryState(state12)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun toNeighborsChild() {
        val callbacks = mock<Callbacks>()
        val inOrder = inOrder(callbacks)

        lateinit var state1: State
        lateinit var state21: State
        lateinit var state2: State

        val machine = createStateMachine {
            state1 = initialState("1") {
                callbacks.listen(this)

                transitionTo<SwitchEvent> {
                    targetState = { state21 }
                    callbacks.listen(this)
                }
            }
            state2 = state("2") {
                callbacks.listen(this)

                state21 = initialState("21") {
                    callbacks.listen(this)
                }
            }
        }

        machine.processEvent(SwitchEvent)

        then(callbacks).should(inOrder).onEntryState(state1)
        then(callbacks).should(inOrder).onTriggeredTransition(SwitchEvent)
        then(callbacks).should(inOrder).onExitState(state1)
        then(callbacks).should(inOrder).onEntryState(state2)
        then(callbacks).should(inOrder).onEntryState(state21)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun childToNeighborsChild() {
        val callbacks = mock<Callbacks>()
        val inOrder = inOrder(callbacks)

        lateinit var state1: State
        lateinit var state11: State
        lateinit var state2: State
        lateinit var state21: State
        lateinit var state22: State

        val machine = createStateMachine {
            state1 = initialState("1") {
                callbacks.listen(this)

                state11 = initialState("11") {
                    callbacks.listen(this)

                    transitionTo<SwitchEvent> {
                        targetState = { state22 }
                        callbacks.listen(this)
                    }
                }
            }
            state2 = state("2") {
                callbacks.listen(this)

                state21 = initialState("21") {
                    callbacks.listen(this)
                }

                state22 = state("22") {
                    callbacks.listen(this)
                }
            }
        }

        machine.processEvent(SwitchEvent)

        then(callbacks).should(inOrder).onEntryState(state1)
        then(callbacks).should(inOrder).onEntryState(state11)
        then(callbacks).should(inOrder).onTriggeredTransition(SwitchEvent)
        then(callbacks).should(inOrder).onExitState(state11)
        then(callbacks).should(inOrder).onExitState(state1)
        then(callbacks).should(inOrder).onEntryState(state2)
        then(callbacks).should(inOrder).onEntryState(state22)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun childToTopLevelNeighbor() {
        val callbacks = mock<Callbacks>()
        val inOrder = inOrder(callbacks)

        lateinit var state1: State
        lateinit var state11: State
        lateinit var state2: State

        val machine = createStateMachine {
            state1 = initialState("1") {
                callbacks.listen(this)

                state11 = initialState("11") {
                    callbacks.listen(this)

                    transitionTo<SwitchEvent> {
                        targetState = { state2 }
                        callbacks.listen(this)
                    }
                }
            }

            state2 = state("2") {
                callbacks.listen(this)
            }
        }

        machine.processEvent(SwitchEvent)

        then(callbacks).should(inOrder).onEntryState(state1)
        then(callbacks).should(inOrder).onEntryState(state11)
        then(callbacks).should(inOrder).onTriggeredTransition(SwitchEvent)
        then(callbacks).should(inOrder).onExitState(state11)
        then(callbacks).should(inOrder).onExitState(state1)
        then(callbacks).should(inOrder).onEntryState(state2)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun childToParent() {
        val callbacks = mock<Callbacks>()
        val inOrder = inOrder(callbacks)

        lateinit var state1: State
        lateinit var state11: State
        lateinit var state12: State

        val machine = createStateMachine {
            logger = StateMachine.Logger { println(it) }

            state1 = initialState("1") {
                callbacks.listen(this)

                state11 = initialState("11") {
                    callbacks.listen(this)

                    transitionTo<SwitchEvent> {
                        targetState = { state12 }
                        callbacks.listen(this)
                    }
                }
                state12 = state("12") {
                    callbacks.listen(this)

                    transitionTo<SwitchEvent> {
                        targetState = { state1 }
                        callbacks.listen(this)
                    }
                }
            }
        }

        machine.processEvent(SwitchEvent)
        machine.processEvent(SwitchEvent)

        then(callbacks).should(inOrder).onEntryState(state1)
        then(callbacks).should(inOrder).onEntryState(state11)
        then(callbacks).should(inOrder).onTriggeredTransition(SwitchEvent)
        then(callbacks).should(inOrder).onExitState(state11)
        then(callbacks).should(inOrder).onEntryState(state12)

        then(callbacks).should(inOrder).onTriggeredTransition(SwitchEvent)
        then(callbacks).should(inOrder).onExitState(state12)
        then(callbacks).should(inOrder).onEntryState(state11)
        then(callbacks).shouldHaveNoMoreInteractions()
    }
}