package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.fail

class TransitionTest : StringSpec({
    "transition direction" {
        val callbacks = mockkCallbacks()

        lateinit var state1: State
        lateinit var state2: State

        val machine = createStateMachine {
            state1 = initialState("1") {
                onEntry {
                    callbacks.onEntryState(this)
                    it.direction.targetState shouldBeSameInstanceAs this
                }

                onExit {
                    if (it.direction.targetState == state2)
                        callbacks.onExitState(this)
                    else
                        fail("incorrect direction ${it.direction}")
                }

                transitionOn<SwitchEvent> {
                    targetState = { state2 }
                    callbacks.listen(this)
                }
            }

            state2 = state("2") { callbacks.listen(this) }
        }

        verifySequenceAndClear(callbacks) { callbacks.onEntryState(state1) }

        machine.processEvent(SwitchEvent)

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEvent)
            callbacks.onExitState(state1)
            callbacks.onEntryState(state2)
        }
    }

    "top level transition" {
        val callbacks = mockkCallbacks()

        lateinit var state2: State

        val machine = createStateMachine {
            transitionOn<SwitchEvent> {
                targetState = { state2 }
                callbacks.listen(this)
            }

            initialState("state1")
            state2 = state("state2") { callbacks.listen(this) }
        }

        machine.processEvent(SwitchEvent)

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEvent)
            callbacks.onEntryState(state2)
        }
    }

    "transition to null target state" {
        val callbacks = mockkCallbacks()

        val machine = createStateMachine {
            initialState("initial") {
                transition<SwitchEvent> {
                    targetState = null
                    callbacks.listen(this)
                }
            }
        }

        machine.processEvent(SwitchEvent)
        verify { callbacks.onTriggeredTransition(SwitchEvent) }
    }

    "transition with shortcut method" {
        lateinit var finalState: FinalState

        val machine = createStateMachine {
            finalState = finalState()
            initialState {
                transition<SwitchEvent>("transition1", finalState)
            }
        }

        machine.processEvent(SwitchEvent)
        finalState.isActive shouldBe true
    }
})