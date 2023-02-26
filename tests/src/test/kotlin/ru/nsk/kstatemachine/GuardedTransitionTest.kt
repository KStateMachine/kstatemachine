package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.mockk.called
import io.mockk.verify
import io.mockk.verifySequence

class GuardedTransitionTest : StringSpec({
    "guarded transition" {
        val callbacks = mockkCallbacks()

        var value = "value1"

        val machine = createTestStateMachine {
            logger = StateMachine.Logger { println(it) }

            val second = state("second")

            initialState("first") {
                transition<SwitchEvent> {
                    guard = {
                        this@initialState.machine.log { "$event $argument" }
                        value == "value2"
                    }
                    targetState = second
                    callbacks.listen(this)
                }
            }
        }

        machine.processEvent(SwitchEvent)
        verify { callbacks wasNot called }

        value = "value2"
        machine.processEvent(SwitchEvent)
        verify { callbacks.onTriggeredTransition(SwitchEvent) }
    }

    "guarded transitionOn() with lateinit state" {
        val callbacks = mockkCallbacks()

        var value = "value1"

        val machine = createTestStateMachine {
            lateinit var second: State

            initialState("first") {
                transitionOn<SwitchEvent> {
                    guard = { value == "value2" }
                    targetState = { second }
                    callbacks.listen(this)
                }
            }

            second = state("second")
        }

        machine.processEvent(SwitchEvent)
        verify { callbacks wasNot called }

        value = "value2"
        machine.processEvent(SwitchEvent)
        verify { callbacks.onTriggeredTransition(SwitchEvent) }
    }

    "guarded transition same event" {
        val callbacks = mockkCallbacks()

        lateinit var state1: State
        lateinit var state2: State
        lateinit var state3: State

        val machine = createTestStateMachine {
            state1 = initialState("state1") {
                callbacks.listen(this)

                transitionOn<SwitchEvent> {
                    guard = { false }
                    targetState = { state2 }
                    callbacks.listen(this)
                }

                transitionOn<SwitchEvent> {
                    guard = { true }
                    targetState = { state3 }
                    callbacks.listen(this)
                }
            }

            state2 = state("state2") { callbacks.listen(this) }
            state3 = state("state3") { callbacks.listen(this) }
        }

        verifySequenceAndClear(callbacks) { callbacks.onEntryState(state1) }

        machine.processEvent(SwitchEvent)

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEvent)
            callbacks.onExitState(state1)
            callbacks.onEntryState(state3)
        }
    }
})