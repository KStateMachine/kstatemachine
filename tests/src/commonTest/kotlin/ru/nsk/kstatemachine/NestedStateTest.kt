package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.mockk.verifySequence

class NestedStateTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "start nested states branch" {
            val callbacks = mockkCallbacks()

            lateinit var firstL1: State
            lateinit var firstL2: State
            val firstL3 = object : DefaultState("firstL3") {}

            createTestStateMachine(coroutineStarterType) {
                firstL1 = initialState("firstL1") {
                    callbacks.listen(this)

                    firstL2 = initialState("firstL2") {
                        callbacks.listen(this)

                        addInitialState(firstL3) {
                            callbacks.listen(this)
                        }
                    }
                }
            }

            verifySequence {
                callbacks.onStateEntry(firstL1)
                callbacks.onStateEntry(firstL2)
                callbacks.onStateEntry(firstL3)
            }
        }

        "exit enter nested states branch" {
            val callbacks = mockkCallbacks()

            lateinit var firstL1: State
            lateinit var secondL1: State
            lateinit var firstL2: State
            lateinit var secondL2: State

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it()) }

                secondL1 = state("secondL1") {
                    callbacks.listen(this)

                    secondL2 = initialState("secondL2") {
                        callbacks.listen(this)
                    }
                }

                firstL1 = initialState("firstL1") {
                    callbacks.listen(this)

                    transition<SwitchEventL1> {
                        targetState = secondL1
                        callbacks.listen(this)
                    }

                    firstL2 = initialState("firstL2") {
                        callbacks.listen(this)
                    }
                }
            }

            verifySequenceAndClear(callbacks) {
                callbacks.onStateEntry(firstL1)
                callbacks.onStateEntry(firstL2)
            }

            machine.processEventBlocking(SwitchEventL1)

            verifySequence {
                callbacks.onTransitionTriggered(SwitchEventL1)
                callbacks.onStateExit(firstL2)
                callbacks.onStateExit(firstL1)
                callbacks.onStateEntry(secondL1)
                callbacks.onStateEntry(secondL2)
            }
        }

        "nested no initial state" {
            val machine = createTestStateMachine(coroutineStarterType, start = false) {
                initialState("firstL1") {
                    state("firstL2")
                }
            }

            shouldThrow<IllegalStateException> { machine.startBlocking() }
        }

        "reenter initial nested state" {
            val callbacks = mockkCallbacks()

            lateinit var state1: State
            lateinit var state11: State
            lateinit var state2: State

            val machine = createTestStateMachine(coroutineStarterType) {
                state1 = initialState("1") {
                    callbacks.listen(this)

                    state11 = initialState("11") {
                        callbacks.listen(this)
                    }

                    transitionOn<SwitchEvent> {
                        targetState = { state2 }
                        callbacks.listen(this)
                    }
                }
                state2 = state("2") {
                    callbacks.listen(this)

                    transitionOn<SwitchEvent> {
                        targetState = { state1 }
                        callbacks.listen(this)
                    }
                }
            }

            verifySequenceAndClear(callbacks) {
                callbacks.onStateEntry(state1)
                callbacks.onStateEntry(state11)
            }

            machine.processEventBlocking(SwitchEvent)
            verifySequenceAndClear(callbacks) {
                callbacks.onTransitionTriggered(SwitchEvent)
                callbacks.onStateExit(state11)
                callbacks.onStateExit(state1)
                callbacks.onStateEntry(state2)
            }

            machine.processEventBlocking(SwitchEvent)
            verifySequenceAndClear(callbacks) {
                callbacks.onTransitionTriggered(SwitchEvent)
                callbacks.onStateExit(state2)
                callbacks.onStateEntry(state1)
                callbacks.onStateEntry(state11)
            }
        }
    }
})