package ru.nsk.kstatemachine

import io.mockk.verifySequence
import org.junit.jupiter.api.Test

class CrossLevelTransitionTest {
    /**
     * Currently transition from self to self is same as targetless transition
     */
    @Test
    fun selfToSelf() {
        val callbacks = mockkCallbacks()

        lateinit var state1: State

        val machine = createStateMachine {
            logger = StateMachine.Logger { println(it) }

            state1 = initialState("1") {
                callbacks.listen(this)

                transitionOn<SwitchEvent> {
                    targetState = { state1 }
                    callbacks.listen(this)
                }
            }
        }

        verifySequenceAndClear(callbacks) { callbacks.onEntryState(state1) }

        machine.processEvent(SwitchEvent)

        verifySequence { callbacks.onTriggeredTransition(SwitchEvent) }
    }

    @Test
    fun selfToSelfWithChildren() {
        val callbacks = mockkCallbacks()

        lateinit var state1: State
        lateinit var state11: State
        lateinit var state12: State

        val machine = createStateMachine {
            logger = StateMachine.Logger { println(it) }

            state1 = initialState("1") {
                callbacks.listen(this)

                transitionOn<SwitchEventL1> {
                    targetState = { state1 }
                    callbacks.listen(this)
                }

                state11 = initialState("11") {
                    callbacks.listen(this)

                    transitionOn<SwitchEventL2> {
                        targetState = { state12 }
                        callbacks.listen(this)
                    }
                }

                state12 = state("12") {
                    callbacks.listen(this)
                }
            }
        }

        verifySequenceAndClear(callbacks) {
            callbacks.onEntryState(state1)
            callbacks.onEntryState(state11)
        }

        machine.processEvent(SwitchEventL2)

        verifySequenceAndClear(callbacks) {
            callbacks.onTriggeredTransition(SwitchEventL2)
            callbacks.onExitState(state11)
            callbacks.onEntryState(state12)
        }

        machine.processEvent(SwitchEventL1)

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEventL1)
            callbacks.onExitState(state12)
            callbacks.onEntryState(state11)
        }
    }

    @Test
    fun parentToChild() {
        val callbacks = mockkCallbacks()

        lateinit var state1: State
        lateinit var state11: State
        lateinit var state12: State

        val machine = createStateMachine {
            logger = StateMachine.Logger { println(it) }

            state1 = initialState("1") {
                callbacks.listen(this)

                transitionOn<SwitchEvent> {
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

        verifySequenceAndClear(callbacks) {
            callbacks.onEntryState(state1)
            callbacks.onEntryState(state11)
        }

        machine.processEvent(SwitchEvent)

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEvent)
            callbacks.onExitState(state11)
            callbacks.onEntryState(state12)
        }
    }

    @Test
    fun toNeighborsChild() {
        val callbacks = mockkCallbacks()

        lateinit var state1: State
        lateinit var state21: State
        lateinit var state2: State

        val machine = createStateMachine {
            state1 = initialState("1") {
                callbacks.listen(this)

                transitionOn<SwitchEvent> {
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

        verifySequenceAndClear(callbacks) { callbacks.onEntryState(state1) }

        machine.processEvent(SwitchEvent)

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEvent)
            callbacks.onExitState(state1)
            callbacks.onEntryState(state2)
            callbacks.onEntryState(state21)
        }
    }

    @Test
    fun childToNeighborsChild() {
        val callbacks = mockkCallbacks()

        lateinit var state1: State
        lateinit var state11: State
        lateinit var state2: State
        lateinit var state22: State

        val machine = createStateMachine {
            state1 = initialState("1") {
                callbacks.listen(this)

                state11 = initialState("11") {
                    callbacks.listen(this)

                    transitionOn<SwitchEvent> {
                        targetState = { state22 }
                        callbacks.listen(this)
                    }
                }
            }
            state2 = state("2") {
                callbacks.listen(this)

                initialState("21") {
                    callbacks.listen(this)
                }

                state22 = state("22") {
                    callbacks.listen(this)
                }
            }
        }

        verifySequenceAndClear(callbacks) {
            callbacks.onEntryState(state1)
            callbacks.onEntryState(state11)
        }

        machine.processEvent(SwitchEvent)

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEvent)
            callbacks.onExitState(state11)
            callbacks.onExitState(state1)
            callbacks.onEntryState(state2)
            callbacks.onEntryState(state22)
        }
    }

    @Test
    fun childToTopLevelNeighbor() {
        val callbacks = mockkCallbacks()

        lateinit var state1: State
        lateinit var state11: State
        lateinit var state2: State

        val machine = createStateMachine {
            state1 = initialState("1") {
                callbacks.listen(this)

                state11 = initialState("11") {
                    callbacks.listen(this)

                    transitionOn<SwitchEvent> {
                        targetState = { state2 }
                        callbacks.listen(this)
                    }
                }
            }

            state2 = state("2") {
                callbacks.listen(this)
            }
        }

        verifySequenceAndClear(callbacks) {
            callbacks.onEntryState(state1)
            callbacks.onEntryState(state11)
        }

        machine.processEvent(SwitchEvent)

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEvent)
            callbacks.onExitState(state11)
            callbacks.onExitState(state1)
            callbacks.onEntryState(state2)
        }
    }

    @Test
    fun childToParent() {
        val callbacks = mockkCallbacks()

        lateinit var state1: State
        lateinit var state11: State
        lateinit var state12: State

        val machine = createStateMachine {
            logger = StateMachine.Logger { println(it) }

            state1 = initialState("1") {
                callbacks.listen(this)

                state11 = initialState("11") {
                    callbacks.listen(this)

                    transitionOn<SwitchEvent> {
                        targetState = { state12 }
                        callbacks.listen(this)
                    }
                }
                state12 = state("12") {
                    callbacks.listen(this)

                    transitionOn<SwitchEvent> {
                        targetState = { state1 }
                        callbacks.listen(this)
                    }
                }
            }
        }

        verifySequenceAndClear(callbacks) {
            callbacks.onEntryState(state1)
            callbacks.onEntryState(state11)
        }

        machine.processEvent(SwitchEvent)

        verifySequenceAndClear(callbacks) {
            callbacks.onTriggeredTransition(SwitchEvent)
            callbacks.onExitState(state11)
            callbacks.onEntryState(state12)
        }

        machine.processEvent(SwitchEvent)

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEvent)
            callbacks.onExitState(state12)
            callbacks.onEntryState(state11)
        }
    }
}