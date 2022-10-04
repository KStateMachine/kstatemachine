package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.verifySequence

class FinishedEventTest : StringSpec({
    "finished event in machine is not working as machine ignores events" {
        val callbacks = mockkCallbacks()
        lateinit var state2: State
        val machine = createStateMachine {
            val final = finalState("final")
            initialState("state1") {
                transition<SwitchEvent>(targetState = final)
            }

            state2 = state("state2") {
                onEntry { error("should not be triggered") }
            }

            onFinished { callbacks.onFinished(this) }
            transitionOn<FinishedEvent> { targetState = { state2 } }
        }

        machine.processEvent(SwitchEvent)

        verifySequence { callbacks.onFinished(machine) }
        machine.isFinished shouldBe true
    }

    "finished event in composite state" {
        val callbacks = mockkCallbacks()
        lateinit var state1: State
        lateinit var state2: State
        val machine = createStateMachine {
            state1 = initialState("state1") {
                val final = finalState("final")
                initialState("state11") {
                    transition<SwitchEvent>(targetState = final)
                }

                onFinished { callbacks.onFinished(this) }
                transitionOn<FinishedEvent> { targetState = { state2 } }
            }
            state2 = state("state2") {
                callbacks.listen(this)
            }
        }

        machine.processEvent(SwitchEvent)

        verifySequence {
            callbacks.onFinished(state1)
            callbacks.onEntryState(state2)
        }
        state1.isFinished shouldBe false
        machine.isFinished shouldBe false
    }
})