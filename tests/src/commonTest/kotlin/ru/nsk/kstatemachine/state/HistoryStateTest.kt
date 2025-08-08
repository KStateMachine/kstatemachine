/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.called
import io.mockk.verify
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.processEventBlocking

class HistoryStateTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "history state cannot have listeners" {
                createTestStateMachine(coroutineStarterType) {
                    initialState()
                    val history = historyState("history")
                    shouldThrowWithMessage<UnsupportedOperationException>(
                        "PseudoState DefaultHistoryState(history) can not have listeners"
                    ) {
                        history.onEntry {}
                    }
                    shouldThrowWithMessage<UnsupportedOperationException>(
                        "PseudoState DefaultHistoryState(history) can not have listeners"
                    ) {
                        history.onExit {}
                    }
                }
            }

            "history default state should be a neighbour state" {
                createTestStateMachine(coroutineStarterType) {
                    lateinit var innerState: State
                    initialState {
                        innerState = initialState("outerState")
                    }
                    shouldThrowWithMessage<IllegalArgumentException>(
                        "Default state DefaultState(outerState) is not a neighbour of DefaultHistoryState(history)"
                    ) {
                        historyState("history", defaultState = innerState)
                    }
                }
            }

            "shallow history in flat machine" {
                val callbacks = mockkCallbacks()
                lateinit var history: HistoryState
                lateinit var state1: State
                lateinit var state2: State

                val machine = createTestStateMachine(coroutineStarterType) {
                    logger = StateMachine.Logger { println(it()) }

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

                verifySequenceAndClear(callbacks) { callbacks.onStateEntry(state1) }
                history.storedState shouldBe state1

                machine.processEventBlocking(SwitchEvent)
                verifySequenceAndClear(callbacks) {
                    callbacks.onStateExit(state1)
                    callbacks.onStateEntry(state2)
                }
                history.storedState shouldBe state2

                machine.processEventBlocking(SwitchEvent)
                // switching to history state in this case does nothing
                verify { callbacks wasNot called }
            }

            "shallow history nested states" {
                val callbacks = mockkCallbacks()
                lateinit var state12: State
                lateinit var state2: State
                lateinit var history: HistoryState

                val machine = createTestStateMachine(coroutineStarterType) {
                    logger = StateMachine.Logger { println(it()) }

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

                machine.processEventBlocking(SwitchEvent)
                verifySequenceAndClear(callbacks) {
                    callbacks.onStateEntry(state12)
                }
                history.storedState shouldBe state12

                machine.processEventBlocking(SwitchEvent)
                verifySequenceAndClear(callbacks) {
                    callbacks.onStateExit(state12)
                }
                history.storedState shouldBe state12

                machine.processEventBlocking(SwitchEvent)
                verifySequenceAndClear(callbacks) {
                    callbacks.onStateEntry(state12)
                }
            }

            "shallow history nested states with default state" {
                val callbacks = mockkCallbacks()
                lateinit var state22: State
                lateinit var history: HistoryState

                val machine = createTestStateMachine(coroutineStarterType) {
                    logger = StateMachine.Logger { println(it()) }

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

                machine.processEventBlocking(SwitchEvent)
                verifySequenceAndClear(callbacks) {
                    callbacks.onStateEntry(state22)
                }
                history.storedState shouldBe state22
            }

            "deep history, entry neighbour state" {
                lateinit var state2: State
                lateinit var state1222: State
                lateinit var history: State

                val machine = createTestStateMachine(coroutineStarterType) {
                    logger = StateMachine.Logger { println(it()) }

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

                machine.processEventBlocking(SwitchEvent)
                machine.processEventBlocking(SwitchEvent) // exit history scope
                machine.processEventBlocking(SwitchEvent) // go back through history

                machine.activeStates().shouldContain(state1222)
            }

            "shallow history, switching inside neighbour state" {
                lateinit var state2: State
                lateinit var state111: State
                lateinit var state112: State
                lateinit var history: State

                val machine = createTestStateMachine(coroutineStarterType) {
                    logger = StateMachine.Logger { println(it()) }

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

                machine.processEventBlocking(SwitchEvent)
                machine.processEventBlocking(SwitchEvent) // exit history scope
                machine.processEventBlocking(SwitchEvent) // go back through history

                machine.activeStates().shouldContain(state111)
            }

            "deep history, switching inside neighbour state" {
                lateinit var state1: State
                lateinit var state11: State
                lateinit var state2: State
                lateinit var state112: State
                lateinit var history: State

                val machine = createTestStateMachine(coroutineStarterType) {
                    logger = StateMachine.Logger { println(it()) }

                    state1 = initialState("state1") {
                        state11 = initialState("state11") {
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

                machine.processEventBlocking(SwitchEvent)
                machine.processEventBlocking(SwitchEvent) // exit history scope
                machine.processEventBlocking(SwitchEvent) // go back through history

                machine.activeStates().shouldContainExactly(state1, state11, state112)
            }
        }
    }
})