/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ru.nsk.kstatemachine.CoroutineStarterType
import ru.nsk.kstatemachine.SwitchEvent
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.statemachine.processEventBlocking

class FinishingStateTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "reenter finished branch" {
            lateinit var state11: State
            lateinit var finalState111: State
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState("state1") {
                    state11 = initialState("state11") {
                        val state112 = state("state112") {
                            transitionOn<SwitchEvent> { targetState = { finalState111 } }
                        }

                        finalState111 = initialFinalState("finalState111") {
                            transition<SwitchEvent>(targetState = state112)
                        }
                    }
                }
            }
            state11.isFinished shouldBe true
            machine.processEvent(SwitchEvent)
            state11.isFinished shouldBe false
            machine.processEvent(SwitchEvent)
            state11.isFinished shouldBe true
        }

        "composite state isFinished is reset on leaving final state" {
            lateinit var state11: State
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState("state1") {
                    state11 = initialState("state11") {
                        val state112 = state("state112")

                        initialFinalState("finalState111") {
                            transition<SwitchEvent>(targetState = state112)
                        }
                    }
                }
            }

            state11.isFinished shouldBe true
            machine.isFinished shouldBe false

            machine.processEventBlocking(SwitchEvent)
            state11.isFinished shouldBe false
        }

        "composite state isFinished is reset on leaving this state" {
            lateinit var state11: State
            lateinit var state12: State
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState("state1") {
                    state11 = initialState("state11") {
                        initialFinalState("finalState111") {
                            transitionOn<SwitchEvent> { targetState = { state12 } }
                        }
                    }
                }
                state12 = state("state12")
            }

            state11.isFinished shouldBe true
            machine.isFinished shouldBe false

            machine.processEventBlocking(SwitchEvent)
            state11.isFinished shouldBe false
        }
    }
})