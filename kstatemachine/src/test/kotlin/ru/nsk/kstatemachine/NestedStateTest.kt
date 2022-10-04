package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.mockk.called
import io.mockk.verifySequence

class NestedStateTest : StringSpec({
    "start nested states branch" {
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

    "exit enter nested states branch" {
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

    "nested no initial state" {
        val machine = createStateMachine(start = false) {
            initialState("firstL1") {
                state("firstL2")
            }
        }

        shouldThrow<IllegalStateException> { machine.start() }
    }

    "reenter initial nested state" {
        val callbacks = mockkCallbacks()

        lateinit var state1: State
        lateinit var state11: State
        lateinit var state2: State

        val machine = createStateMachine {
            state1 = initialState("1") {
                callbacks.listen(this)

                state11 = initialState("11") {
                    callbacks.listen(this)
                }

                transitionOn<SwitchEvent> {
                    targetState = { state2 }
                    callbacks.listen(this)
                }
            }
            state2 = state("2") {
                callbacks.listen(this)

                transitionOn<SwitchEvent> {
                    targetState = { state1 }
                    callbacks.listen(this)
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
            callbacks.onExitState(state1)
            callbacks.onEntryState(state2)
        }

        machine.processEvent(SwitchEvent)
        verifySequenceAndClear(callbacks) {
            callbacks.onTriggeredTransition(SwitchEvent)
            callbacks.onExitState(state2)
            callbacks.onEntryState(state1)
            callbacks.onEntryState(state11)
        }
    }
})