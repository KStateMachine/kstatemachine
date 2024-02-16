package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.should

class ActiveLeafStatesTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "activeLeafStates()" {
            lateinit var state1: State
            lateinit var state2: State
            lateinit var state211: State

            val machine = createTestStateMachine(coroutineStarterType) {
                state1 = initialState("state1") {
                    transitionOn<SwitchEvent> {
                        targetState = { state2 }
                    }
                }
                state2 = state("state2") {
                    initialState("state21") {
                        state211 = addInitialState(createTestStateMachine(coroutineStarterType, start = false) {
                            // should not be included
                            initialState("state2111")
                        })
                    }
                }
            }

            machine.activeLeafStates() should containExactly(state1)

            machine.processEventBlocking(SwitchEvent)

            machine.activeLeafStates() should containExactly(state211)
            state2.activeLeafStates() should containExactly(state211)
        }

        "activeLeafStates() in parallel child mode" {
            lateinit var state1: State
            lateinit var state2: State

            val machine = createTestStateMachine(coroutineStarterType, childMode = ChildMode.PARALLEL) {
                state1 = state()
                state2 = state()
            }

            machine.activeLeafStates() should containExactly(state1, state2)
        }

        "activeLeafStates() do not include nested machines states" {
            lateinit var nestedMachine: State
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState {
                    nestedMachine = addInitialState(createTestStateMachine(coroutineStarterType) {
                        initialState()
                    })
                }
            }

            machine.activeLeafStates() should containExactly(nestedMachine)
        }
    }
})