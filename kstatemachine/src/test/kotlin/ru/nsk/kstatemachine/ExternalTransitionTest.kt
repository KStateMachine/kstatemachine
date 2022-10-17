package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import ru.nsk.kstatemachine.TransitionType.EXTERNAL

class ExternalTransitionTest : StringSpec({
    "external transition" {
        lateinit var state1: State
        lateinit var state2: State

        val machine = createStateMachine {
            state1 = initialState("state1") {
                transitionOn<SwitchEvent> {
                    targetState = { state2 }
                    type = EXTERNAL
                }
            }
            state2 = state("state2")
        }
        machine.processEvent(SwitchEvent)
    }
})