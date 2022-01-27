package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec

class HistoryStateTest : StringSpec({
    "shallow history in flat machine" {
        val callbacks = mockkCallbacks()
        lateinit var history : HistoryState
        lateinit var state1 : State
        lateinit var state2 : State

        val machine = createStateMachine {
            state1 = initialState {
                transitionOn<SwitchEvent> { targetState = { state2 } }
                callbacks.listen(this)
            }
            state2 = state {
                transitionOn<SwitchEvent> { targetState = { history } }
                callbacks.listen(this)
            }
            history = historyState()
        }


        verifySequenceAndClear(callbacks) { callbacks.onEntryState(state1) }
        machine.processEvent(SwitchEvent)
        verifySequenceAndClear(callbacks) { callbacks.onEntryState(state2) }

        machine.processEvent(SwitchEvent) // switching to history state, we should go to previous state
        verifySequenceAndClear(callbacks) { callbacks.onEntryState(state1) }
    }

    "shallow history nested states" {
        val callbacks = mockkCallbacks()
        lateinit var state1: State
        lateinit var state12: State
        lateinit var state2: State
        lateinit var history: HistoryState

        val machine = createStateMachine {
            state1 = initialState("state1") {
                initialState("state11") {
                    transitionOn<SwitchEvent> {
                        targetState = { state12 }
                    }
                }
                state12 = state("state12") {
                    transitionOn<SwitchEvent> {
                        targetState = { state2 }
                    }
                }

                history = historyState("shallow history")
            }

            state2 = state("state2") {
                transitionOn<SwitchEvent> {
                    targetState = { history }
                }
            }
        }

        machine.processEvent(SwitchEvent)
        machine.processEvent(SwitchEvent)
        machine.processEvent(SwitchEvent)
    }

    "deep history" {
        TODO()
    }
})