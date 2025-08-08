/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.visitors

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.should
import ru.nsk.kstatemachine.CoroutineStarterType
import ru.nsk.kstatemachine.SwitchEvent
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.processEventBlocking

class GetActiveStatesVisitorTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "activeStates()" {
                lateinit var state1: State
                lateinit var state2: State
                lateinit var state21: State
                lateinit var state211: State

                val machine = createTestStateMachine(coroutineStarterType) {
                    state1 = initialState("state1") {
                        transitionOn<SwitchEvent> {
                            targetState = { state2 }
                        }
                    }
                    state2 = state("state2") {
                        state21 = initialState("state21") {
                            state211 = addInitialState(createTestStateMachine(coroutineStarterType, start = false) {
                                // should not be included
                                initialState("state2111")
                            })
                        }
                    }
                }

                machine.activeStates(true) should containExactly(machine, state1)
                machine.activeStates() should containExactly(state1)

                machine.processEventBlocking(SwitchEvent)

                machine.activeStates(true) should containExactly(machine, state2, state21, state211)
                machine.activeStates() should containExactly(state2, state21, state211)

                state2.activeStates(true) should containExactly(state2, state21, state211)
                state2.activeStates() should containExactly(state21, state211)
            }

            "activeStates() in parallel child mode" {
                lateinit var state1: State
                lateinit var state2: State

                val machine = createTestStateMachine(coroutineStarterType, childMode = ChildMode.PARALLEL) {
                    state1 = state()
                    state2 = state()
                }

                machine.activeStates(true) should containExactly(machine, state1, state2)
                machine.activeStates() should containExactly(state1, state2)
            }

            "activeStates() do not include nested machines states" {
                lateinit var initialState: State
                lateinit var nestedMachine: State
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState = initialState {
                        nestedMachine = addInitialState(createTestStateMachine(coroutineStarterType) {
                            initialState()
                        })
                    }
                }

                machine.activeStates() should containExactly(initialState, nestedMachine)
            }
        }
    }
})