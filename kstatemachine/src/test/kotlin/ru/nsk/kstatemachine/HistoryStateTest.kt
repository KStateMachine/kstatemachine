package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import java.lang.IllegalArgumentException

class HistoryStateTest : StringSpec({
    "shallow history" {
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

    "shallow history in flat machine" {
        lateinit var history : HistoryState
        lateinit var state2 : State

        val machine = createStateMachine {
            initialState {
                transitionOn<SwitchEvent> { targetState = { state2 } }
            }
            state2 = state {
                transitionOn<SwitchEvent> { targetState = { history } }
            }
            history = historyState()
        }

        machine.processEvent(SwitchEvent)
        machine.processEvent(SwitchEvent)


    }

    "more than one HistoryState not allowed" {
        shouldThrow<IllegalArgumentException> {
            createStateMachine {
                initialState("state1")
                historyState()
                historyState()
            }
        }
    }

    "deep history" {

    }
})