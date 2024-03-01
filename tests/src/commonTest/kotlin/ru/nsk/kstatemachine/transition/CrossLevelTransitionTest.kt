package ru.nsk.kstatemachine.transition

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.verifySequence
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.processEventBlocking

class CrossLevelTransitionTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        // Transition from self to self is same as target-less transition
        "self to self" {
            val callbacks = mockkCallbacks()

            lateinit var state1: State

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it()) }

                state1 = initialState("1") {
                    callbacks.listen(this)

                    transitionOn<SwitchEvent> {
                        targetState = { state1 }
                        callbacks.listen(this)
                    }
                }
            }

            verifySequenceAndClear(callbacks) { callbacks.onStateEntry(state1) }

            machine.processEventBlocking(SwitchEvent)

            verifySequence { callbacks.onTransitionTriggered(SwitchEvent) }
        }

        "self to self with children" {
            val callbacks = mockkCallbacks()

            lateinit var state1: State
            lateinit var state11: State
            lateinit var state12: State

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it()) }

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
                callbacks.onStateEntry(state1)
                callbacks.onStateEntry(state11)
            }

            machine.processEventBlocking(SwitchEventL2)

            verifySequenceAndClear(callbacks) {
                callbacks.onTransitionTriggered(SwitchEventL2)
                callbacks.onStateExit(state11)
                callbacks.onStateEntry(state12)
            }

            machine.processEventBlocking(SwitchEventL1)

            verifySequence {
                callbacks.onTransitionTriggered(SwitchEventL1)
                callbacks.onStateExit(state12)
                callbacks.onStateEntry(state11)
            }
        }

        "parent to child" {
            val callbacks = mockkCallbacks()

            lateinit var state1: State
            lateinit var state11: State
            lateinit var state12: State

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it()) }

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
                callbacks.onStateEntry(state1)
                callbacks.onStateEntry(state11)
            }

            machine.processEventBlocking(SwitchEvent)

            verifySequence {
                callbacks.onTransitionTriggered(SwitchEvent)
                callbacks.onStateExit(state11)
                callbacks.onStateEntry(state12)
            }
        }

        "to neighbors child" {
            val callbacks = mockkCallbacks()

            lateinit var state1: State
            lateinit var state21: State
            lateinit var state2: State

            val machine = createTestStateMachine(coroutineStarterType) {
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

            verifySequenceAndClear(callbacks) { callbacks.onStateEntry(state1) }

            machine.processEventBlocking(SwitchEvent)

            verifySequence {
                callbacks.onTransitionTriggered(SwitchEvent)
                callbacks.onStateExit(state1)
                callbacks.onStateEntry(state2)
                callbacks.onStateEntry(state21)
            }
        }

        "child to neighbors child" {
            val callbacks = mockkCallbacks()

            lateinit var state1: State
            lateinit var state11: State
            lateinit var state2: State
            lateinit var state22: State

            val machine = createTestStateMachine(coroutineStarterType) {
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
                callbacks.onStateEntry(state1)
                callbacks.onStateEntry(state11)
            }

            machine.processEventBlocking(SwitchEvent)

            verifySequence {
                callbacks.onTransitionTriggered(SwitchEvent)
                callbacks.onStateExit(state11)
                callbacks.onStateExit(state1)
                callbacks.onStateEntry(state2)
                callbacks.onStateEntry(state22)
            }
        }

        "child to top level neighbor" {
            val callbacks = mockkCallbacks()

            lateinit var state1: State
            lateinit var state11: State
            lateinit var state2: State

            val machine = createTestStateMachine(coroutineStarterType) {
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
                callbacks.onStateEntry(state1)
                callbacks.onStateEntry(state11)
            }

            machine.processEventBlocking(SwitchEvent)

            verifySequence {
                callbacks.onTransitionTriggered(SwitchEvent)
                callbacks.onStateExit(state11)
                callbacks.onStateExit(state1)
                callbacks.onStateEntry(state2)
            }
        }

        "child to parent" {
            val callbacks = mockkCallbacks()

            lateinit var state1: State
            lateinit var state11: State
            lateinit var state12: State

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it()) }

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
                callbacks.onStateEntry(state1)
                callbacks.onStateEntry(state11)
            }

            machine.processEventBlocking(SwitchEvent)

            verifySequenceAndClear(callbacks) {
                callbacks.onTransitionTriggered(SwitchEvent)
                callbacks.onStateExit(state11)
                callbacks.onStateEntry(state12)
            }

            machine.processEventBlocking(SwitchEvent)

            verifySequence {
                callbacks.onTransitionTriggered(SwitchEvent)
                callbacks.onStateExit(state12)
                callbacks.onStateEntry(state11)
            }
        }

        "transition to parallel state" {
            lateinit var state2: State
            lateinit var state21: State
            lateinit var state22: State
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState("state1") {
                    transitionOn<SwitchEvent> { targetState = { state22 } }
                }

                state2 = state("state2", childMode = ChildMode.PARALLEL) {
                    state21 = state("state21")
                    state22 = state("state22")
                }
            }
            machine.processEvent(SwitchEvent)

            machine.activeStates().shouldContainExactly(state2, state21, state22)
        }

        "transition to nested state machine" {
            lateinit var state21: State
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState("state1") {
                    transitionOn<SwitchEvent> { targetState = { state21 } }
                }
                createTestStateMachine(coroutineStarterType) {
                    state21 = initialState("state21")
                }
            }
            shouldThrow<IllegalStateException> {
                machine.processEvent(SwitchEvent)
            }
        }
    }
})