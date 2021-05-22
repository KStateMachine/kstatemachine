package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.verifySequence
import org.junit.jupiter.api.Test

class NestedStateTest {
    @Test
    fun startNestedStatesBranch() {
        val callbacks = mockkCallbacks()

        lateinit var firstL1: State
        lateinit var firstL2: State
        val firstL3 = object : DefaultState("firstL3") {}

        createStateMachine {
            firstL1 = initialState("firstL1") {
                callbacks.listen(this)

                firstL2 = initialState("firstL2") {
                    callbacks.listen(this)

                    addInitialState(firstL3) {
                        callbacks.listen(this)
                    }
                }
            }
        }

        verifySequence {
            callbacks.onEntryState(firstL1)
            callbacks.onEntryState(firstL2)
            callbacks.onEntryState(firstL3)
        }
    }

    @Test
    fun exitEnterNestedStatesBranch() {
        val callbacks = mockkCallbacks()

        lateinit var firstL1: State
        lateinit var secondL1: State
        lateinit var firstL2: State
        lateinit var secondL2: State

        val machine = createStateMachine {
            logger = StateMachine.Logger { println(it) }

            secondL1 = state("secondL1") {
                callbacks.listen(this)

                secondL2 = initialState("secondL2") {
                    callbacks.listen(this)
                }
            }

            firstL1 = initialState("firstL1") {
                callbacks.listen(this)

                transition<SwitchEventL1> {
                    targetState = secondL1
                    callbacks.listen(this)
                }

                firstL2 = initialState("firstL2") {
                    callbacks.listen(this)
                }
            }
        }

        verifySequenceAndClear(callbacks) {
            callbacks.onEntryState(firstL1)
            callbacks.onEntryState(firstL2)
        }

        machine.processEvent(SwitchEventL1)

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEventL1)
            callbacks.onExitState(firstL2)
            callbacks.onExitState(firstL1)
            callbacks.onEntryState(secondL1)
            callbacks.onEntryState(secondL2)
        }
    }

    @Test
    fun nestedStateFinishL2() {
        val callbacks = mockkCallbacks()

        lateinit var initialL1: State
        lateinit var finalL1: State
        lateinit var initialL2: State
        lateinit var finalL2: State

        val machine = createStateMachine {
            logger = StateMachine.Logger { println(it) }

            finalL1 = finalState("finalL1") {
                callbacks.listen(this)
            }

            initialL1 = initialState("initialL1") {
                callbacks.listen(this)

                transition<SwitchEventL1> {
                    targetState = finalL1
                    callbacks.listen(this)
                }

                finalL2 = finalState("finalL2") {
                    callbacks.listen(this)
                }

                initialL2 = initialState("initialL2") {
                    callbacks.listen(this)

                    transition<SwitchEventL2> {
                        targetState = finalL2
                        callbacks.listen(this)
                    }
                }
            }

            onFinished { callbacks.onFinished(this) }
        }

        verifySequenceAndClear(callbacks) {
            callbacks.onEntryState(initialL1)
            callbacks.onEntryState(initialL2)
        }

        machine.processEvent(SwitchEventL2)

        verifySequenceAndClear(callbacks) {
            callbacks.onTriggeredTransition(SwitchEventL2)
            callbacks.onExitState(initialL2)
            callbacks.onEntryState(finalL2)
            callbacks.onFinished(initialL1)
        }

        machine.processEvent(SwitchEventL1)

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEventL1)
            callbacks.onExitState(finalL2)
            callbacks.onExitState(initialL1)
            callbacks.onEntryState(finalL1)
            callbacks.onFinished(machine)
        }
    }

    @Test
    fun nestedNoInitialState() {
        val machine = createStateMachine(start = false) {
            initialState("firstL1") {
                state("firstL2")
            }
        }

        shouldThrow<IllegalStateException> { machine.start() }
    }
}