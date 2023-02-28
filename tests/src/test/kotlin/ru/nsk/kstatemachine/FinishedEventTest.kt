package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.verifySequence

class FinishedEventTest : StringSpec({
    CoroutineStarterType.values().forEach { coroutineStarterType ->
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

                onFinished { callbacks.onFinished(this) }
                transitionOn<FinishedEvent> { targetState = { state2 } }
            }

            machine.processEvent(SwitchEvent)

            verifySequence { callbacks.onFinished(machine) }
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
                callbacks.onTriggeredTransition(ofType<FinishedEvent>())
            }
        }

        "FinishedEvent in parallel child mode" {
            val callbacks = mockkCallbacks()

            createTestStateMachine(coroutineStarterType) {
                initialState(childMode = ChildMode.PARALLEL) {
                    state("state1") {
                        setInitialState(finalState("state11"))
                    }
                    state("state2") {
                        setInitialState(finalState("state21"))
                    }
                    transition<FinishedEvent> {
                        callbacks.listen(this)
                    }
                }
            }
            verifySequence {
                callbacks.onTriggeredTransition(ofType<FinishedEvent>())
            }
        }

        "FinishedEvent from start" {
            val callbacks = mockkCallbacks()

            createTestStateMachine(coroutineStarterType) {
                initialState {
                    setInitialState(finalState())
                    transition<FinishedEvent> {
                        onTriggered {
                            it.event.data shouldBe null
                            callbacks.onTriggeredTransition(it.event)
                        }
                    }
                }
            }

            verifySequence {
                callbacks.onTriggeredTransition(ofType<FinishedEvent>())
            }
        }
    }
})