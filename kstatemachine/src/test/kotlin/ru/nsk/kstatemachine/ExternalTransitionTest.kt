package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import ru.nsk.kstatemachine.TransitionType.EXTERNAL
import ru.nsk.kstatemachine.TransitionType.LOCAL

class ExternalTransitionTest : StringSpec({
    "external transition on machine level" {
        val callbacks = mockkCallbacks()
        lateinit var state1: State
        lateinit var state2: State

        val machine = createStateMachine {
            callbacks.listen(this)
            state1 = initialState("state1") {
                callbacks.listen(this)
                transitionOn<SwitchEvent> {
                    targetState = { state2 }
                    type = EXTERNAL
                }
            }
            state2 = state("state2") {
                callbacks.listen(this)
            }
        }
        verifySequenceAndClear(callbacks) {
            callbacks.onEntryState(machine)
            callbacks.onEntryState(state1)
        }

        machine.processEvent(SwitchEvent)

        verifySequenceAndClear(callbacks) {
            callbacks.onExitState(state1)
            callbacks.onEntryState(state2)
        }
    }

    "external transition" {
        val callbacks = mockkCallbacks()
        lateinit var state1: State
        lateinit var state11: State
        lateinit var state12: State

        val machine = createStateMachine {
            callbacks.listen(this)

            state1 = initialState("state1") {
                callbacks.listen(this)

                state11 = initialState("state11") {
                    callbacks.listen(this)
                    transitionOn<SwitchEvent> {
                        targetState = { state12 }
                        type = EXTERNAL
                    }
                }
                state12 = state("state12") {
                    callbacks.listen(this)
                }
            }
        }
        verifySequenceAndClear(callbacks) {
            callbacks.onEntryState(machine)
            callbacks.onEntryState(state1)
            callbacks.onEntryState(state11)
        }

        machine.processEvent(SwitchEvent)

        verifySequenceAndClear(callbacks) {
            callbacks.onExitState(state11)
            callbacks.onExitState(state1)
            callbacks.onEntryState(state1)
            callbacks.onEntryState(state12)
        }
    }

    "external transition self targeted" {
        val callbacks = mockkCallbacks()
        lateinit var state1: State
        lateinit var state11: State

        val machine = createStateMachine {
            callbacks.listen(this)

            state1 = initialState("state1") {
                callbacks.listen(this)

                state11 = initialState("state11") {
                    callbacks.listen(this)
                    transitionOn<SwitchEvent> {
                        targetState = { state11 }
                        type = EXTERNAL
                    }
                }
                state("state12") {
                    callbacks.listen(this)
                }
            }
        }
        verifySequenceAndClear(callbacks) {
            callbacks.onEntryState(machine)
            callbacks.onEntryState(state1)
            callbacks.onEntryState(state11)
        }

        machine.processEvent(SwitchEvent)
        verifySequenceAndClear(callbacks) {
            callbacks.onExitState(state11)
            callbacks.onEntryState(state11)
        }
    }

    listOf(EXTERNAL, LOCAL).forEach { transitionType ->
        "external target-less transition same as local" {
            val callbacks = mockkCallbacks()
            lateinit var state1: State
            lateinit var state11: State
            lateinit var state12: State

            val machine = createStateMachine {
                callbacks.listen(this)

                state1 = initialState("state1") {
                    callbacks.listen(this)

                    state11 = initialState("state11") {
                        callbacks.listen(this)
                        transition<SwitchEvent> {
                            type = transitionType
                            callbacks.listen(this)
                        }
                    }
                    state("state12") {
                        callbacks.listen(this)
                    }
                }
            }
            verifySequenceAndClear(callbacks) {
                callbacks.onEntryState(machine)
                callbacks.onEntryState(state1)
                callbacks.onEntryState(state11)
            }

            machine.processEvent(SwitchEvent)
            verifySequenceAndClear(callbacks) {
                callbacks.onTriggeredTransition(SwitchEvent)
            }
        }
    }
})