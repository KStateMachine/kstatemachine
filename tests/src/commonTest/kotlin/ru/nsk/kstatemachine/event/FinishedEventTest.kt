/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.event

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.verifySequence
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.processEventBlocking
import ru.nsk.kstatemachine.transition.EventAndArgument
import ru.nsk.kstatemachine.transition.onTriggered
import ru.nsk.kstatemachine.visitors.export.exportToPlantUml

class FinishedEventTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "FinishedEvent in machine is not working as machine ignores events" {
            val callbacks = mockkCallbacks()
            lateinit var state2: State
            val machine = createTestStateMachine(coroutineStarterType) {
                val final = finalState("final")
                initialState("state1") {
                    transition<SwitchEvent>(targetState = final)
                }

                state2 = state("state2") {
                    onEntry { error("should not be triggered") }
                }

                onFinished { callbacks.onStateFinished(this) }
                transitionOn<FinishedEvent> { targetState = { state2 } }
            }

            machine.processEvent(SwitchEvent)

            verifySequence { callbacks.onStateFinished(machine) }
            machine.isFinished shouldBe true
        }

        "FinishedEvent in composite state" {
            val callbacks = mockkCallbacks()
            lateinit var state1: State
            lateinit var state2: State
            val machine = createTestStateMachine(coroutineStarterType) {
                state1 = initialState("state1") {
                    val final = finalState("final")
                    initialState("state11") {
                        transition<SwitchEvent>(targetState = final)
                    }

                    onFinished { callbacks.onStateFinished(this) }
                    transitionOn<FinishedEvent> { targetState = { state2 } }
                }
                state2 = state("state2") {
                    callbacks.listen(this)
                }
            }

            machine.processEvent(SwitchEvent)

            verifySequence {
                callbacks.onStateFinished(state1)
                callbacks.onStateEntry(state2)
            }
            state1.isActive shouldBe false
            state1.isFinished shouldBe false
            machine.isFinished shouldBe false
        }

        "FinishedEvent relies on queuePendingEventHandler" {
            val callbacks = mockkCallbacks()
            lateinit var state1: State
            lateinit var state2: State
            val machine = createTestStateMachine(coroutineStarterType) {
                pendingEventHandler = StateMachine.PendingEventHandler { /*ignore FinishedEvent*/ }
                state1 = initialState("state1") {
                    val final = finalState("final")
                    initialState("state11") {
                        transition<SwitchEvent>(targetState = final)
                    }

                    onFinished { callbacks.onStateFinished(this) }
                    transitionOn<FinishedEvent> { targetState = { state2 } }
                }
                state2 = state("state2") {
                    callbacks.listen(this)
                }
            }

            machine.processEvent(SwitchEvent)

            verifySequence {
                callbacks.onStateFinished(state1)
            }
            state1.isActive shouldBe true
            state1.isFinished shouldBe true
            machine.isFinished shouldBe false
        }

        "FinishedEvent with data" {
            val callbacks = mockkCallbacks()

            data class IntEvent(override val data: Int) : DataEvent<Int>

            val intData = 42

            val machine = createTestStateMachine(coroutineStarterType) {
                val state2 = state("state2")

                initialState("state1") {
                    val childFinal = finalDataState<Int>("state11")

                    initialState("state12") {
                        dataTransition<IntEvent, Int> {
                            targetState = childFinal
                        }
                    }

                    transitionOn<FinishedEvent> {
                        targetState = {
                            event.data shouldBe intData
                            state2
                        }
                        callbacks.listen(this)
                    }
                }
            }
            machine.processEvent(IntEvent(intData))
            verifySequence {
                callbacks.onTransitionTriggered(ofType<FinishedEvent>())
            }
        }

        "FinishedEvent in parallel child mode" {
            val callbacks = mockkCallbacks()

            createTestStateMachine(coroutineStarterType) {
                initialState(childMode = ChildMode.PARALLEL) {
                    state("state1") {
                        initialFinalState("state11")
                    }
                    state("state2") {
                        initialFinalState("state21")
                    }
                    transition<FinishedEvent> {
                        callbacks.listen(this)
                    }
                }
            }
            verifySequence {
                callbacks.onTransitionTriggered(ofType<FinishedEvent>())
            }
        }

        "FinishedEvent from start" {
            val callbacks = mockkCallbacks()

            createTestStateMachine(coroutineStarterType) {
                initialState {
                    initialFinalState()
                    transition<FinishedEvent> {
                        onTriggered {
                            it.event.data shouldBe null
                            callbacks.onTransitionTriggered(it.event)
                        }
                    }
                }
            }

            verifySequence {
                callbacks.onTransitionTriggered(ofType<FinishedEvent>())
            }
        }
    }
})