package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.called
import io.mockk.verify

class HistoryStateTest : StringSpec({
    "history state cannot have listeners" {
        createTestStateMachine {
            initialState()
            val history = historyState()
            shouldThrow<UnsupportedOperationException> {
                history.onEntry {}
            }
            shouldThrow<UnsupportedOperationException> {
                history.onExit {}
            }
        }
    }

    "history default state should be a neighbour state" {
        createTestStateMachine {
            lateinit var innerState: State
            initialState {
                innerState = initialState()
            }
            shouldThrow<IllegalArgumentException> {
                historyState(defaultState = innerState)
            }
        }
    }

    "shallow history in flat machine" {
        val callbacks = mockkCallbacks()
        lateinit var history: HistoryState
        lateinit var state1: State
        lateinit var state2: State

        val machine = createTestStateMachine {
            logger = StateMachine.Logger { println(it) }

            state1 = initialState("state1") {
                transitionOn<SwitchEvent> { targetState = { state2 } }
                callbacks.listen(this)
            }
            state2 = state("state2") {
                transitionOn<SwitchEvent> { targetState = { history } }
                callbacks.listen(this)
            }
            history = historyState("history")
        }

        verifySequenceAndClear(callbacks) { callbacks.onEntryState(state1) }
        history.storedState shouldBe state1

        machine.processEvent(SwitchEvent)
        verifySequenceAndClear(callbacks) {
            callbacks.onExitState(state1)
            callbacks.onEntryState(state2)
        }
        history.storedState shouldBe state2

        machine.processEvent(SwitchEvent)
        // switching to history state in this case does nothing
        verify { callbacks wasNot called }
    }

    "shallow history nested states" {
        val callbacks = mockkCallbacks()
        lateinit var state12: State
        lateinit var state2: State
        lateinit var history: HistoryState

        val machine = createTestStateMachine {
            logger = StateMachine.Logger { println(it) }

            initialState("state1") {
                initialState("state11") {
                    transitionOn<SwitchEvent> { targetState = { state12 } }
                }
                state12 = state("state12") {
                    callbacks.listen(this)
                    transitionOn<SwitchEvent> { targetState = { state2 } }
                }

                history = historyState("shallow history")
            }

            state2 = state("state2") {
                transitionOn<SwitchEvent> { targetState = { history } }
            }
        }

        machine.processEvent(SwitchEvent)
        verifySequenceAndClear(callbacks) {
            callbacks.onEntryState(state12)
        }
        history.storedState shouldBe state12

        machine.processEvent(SwitchEvent)
        verifySequenceAndClear(callbacks) {
            callbacks.onExitState(state12)
        }
        history.storedState shouldBe state12

        machine.processEvent(SwitchEvent)
        verifySequenceAndClear(callbacks) {
            callbacks.onEntryState(state12)
        }
    }

    "shallow history nested states with default state" {
        val callbacks = mockkCallbacks()
        lateinit var state22: State
        lateinit var history: HistoryState

        val machine = createTestStateMachine {
            logger = StateMachine.Logger { println(it) }

            initialState("state1") {
                initialState("state11") {
                    transitionOn<SwitchEvent> { targetState = { history } }
                }
            }

            state("state2") {
                state("state22")
                state22 = state("state21") {
                    callbacks.listen(this)
                }

                history = historyState("shallow history", defaultState = state22)
            }
        }

        history.storedState shouldBe state22

        machine.processEvent(SwitchEvent)
        verifySequenceAndClear(callbacks) {
            callbacks.onEntryState(state22)
        }
        history.storedState shouldBe state22
    }

    "deep history, entry neighbour state" {
        lateinit var state2: State
        lateinit var state1222: State
        lateinit var history: State

        val machine = createTestStateMachine {
            logger = StateMachine.Logger { println(it) }

            initialState("state1") {
                initialState("state11") {
                    transitionOn<SwitchEvent> { targetState = { state1222 } }
                }

                state("state12") {
                    initialState("state121")
                    state("state122") {
                        initialState("state1221")
                        state1222 = state("1222") {
                            transitionOn<SwitchEvent> { targetState = { state2 } }
                        }
                    }
                }

                history = historyState(historyType = HistoryType.DEEP)
            }
            state2 = state("state2") {
                transitionOn<SwitchEvent> { targetState = { history } }
            }
        }

        machine.processEvent(SwitchEvent)
        machine.processEvent(SwitchEvent) // exit history scope
        machine.processEvent(SwitchEvent) // go back through history

        machine.activeStates().shouldContain(state1222)
    }

    "shallow history, switching inside neighbour state" {
        lateinit var state2: State
        lateinit var state111: State
        lateinit var state112: State
        lateinit var history: State

        val machine = createTestStateMachine {
            logger = StateMachine.Logger { println(it) }

            initialState("state1") {
                initialState("state11") {
                    state111 = initialState("state111") {
                        transitionOn<SwitchEvent> { targetState = { state112 } }
                    }
                    state112 = state("state112") {
                        transitionOn<SwitchEvent> { targetState = { state2 } }
                    }
                }

                history = historyState(historyType = HistoryType.SHALLOW)
            }
            state2 = state("state2") {
                transitionOn<SwitchEvent> { targetState = { history } }
            }
        }

        machine.processEvent(SwitchEvent)
        machine.processEvent(SwitchEvent) // exit history scope
        machine.processEvent(SwitchEvent) // go back through history

        machine.activeStates().shouldContain(state111)
    }

    "deep history, switching inside neighbour state" {
        lateinit var state2: State
        lateinit var state112: State
        lateinit var history: State

        val machine = createTestStateMachine {
            logger = StateMachine.Logger { println(it) }

            initialState("state1") {
                initialState("state11") {
                    initialState("state111") {
                        transitionOn<SwitchEvent> { targetState = { state112 } }
                    }
                    state112 = state("state112") {
                        transitionOn<SwitchEvent> { targetState = { state2 } }
                    }
                }

                history = historyState(historyType = HistoryType.DEEP)
            }
            state2 = state("state2") {
                transitionOn<SwitchEvent> { targetState = { history } }
            }
        }

        machine.processEvent(SwitchEvent)
        machine.processEvent(SwitchEvent) // exit history scope
        machine.processEvent(SwitchEvent) // go back through history

        machine.activeStates().shouldContain(state112)
    }
})