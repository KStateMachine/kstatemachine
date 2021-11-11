package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.should

class ActiveStatesTest : StringSpec({
    "activeStates()" {
        lateinit var state1: State
        lateinit var state2: State
        lateinit var state21: State
        lateinit var state211: State

        val machine = createStateMachine {
            state1 = initialState("state1") {
                transitionOn<SwitchEvent> {
                    targetState = { state2 }
                }
            }
            state2 = state("state2") {
                state21 = initialState("state21") {
                    state211 = addInitialState(createStateMachine(start = false) {
                        // should not be included
                        initialState("state2111")
                    })
                }
            }
        }

        machine.activeStates(true) should containExactly(machine, state1)
        machine.activeStates() should containExactly(state1)

        machine.processEvent(SwitchEvent)

        machine.activeStates(true) should containExactly(machine, state2, state21, state211)
        machine.activeStates() should containExactly(state2, state21, state211)

        state2.activeStates(true) should containExactly(state2, state21, state211)
        state2.activeStates() should containExactly(state21, state211)
    }

    "activeStates() in parallel child mode" {
        lateinit var state1: State
        lateinit var state2: State

        val machine = createStateMachine(childMode = ChildMode.PARALLEL) {
            state1 = state()
            state2 = state()
        }

        machine.activeStates(true) should containExactly(machine, state1, state2)
        machine.activeStates() should containExactly(state1, state2)
    }

    "activeStates() do not include nested machines states" {
        TODO()
    }
})