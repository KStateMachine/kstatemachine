package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.verifySequence

class ParallelTargetStatesTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "transition targets multiple parallel states children" {
            val callbacks = mockkCallbacks()

            lateinit var state1: State
            lateinit var state2: State
            lateinit var state21: State
            lateinit var state212: State
            lateinit var state22: State
            lateinit var state222: State

            val machine = createTestStateMachine(coroutineStarterType) {
                callbacks.listen(this)
                state1 = initialState("state1") {
                    callbacks.listen(this)
                    transitionConditionally<SwitchEvent> {
                        direction = { targetParallelStates(state212, state222) }
                    }
                }
                state2 = state("state2", childMode = ChildMode.PARALLEL) {
                    callbacks.listen(this)
                    state21 = state("state21") {
                        callbacks.listen(this)
                        initialState("state211") {
                            callbacks.listen(this)
                        }
                        state212 = state("state212") {
                            callbacks.listen(this)
                        }
                    }
                    state22 = state("state22") {
                        callbacks.listen(this)
                        initialState("state221") {
                            callbacks.listen(this)
                        }
                        state222 = state("state222") {
                            callbacks.listen(this)
                        }
                    }
                }
            }

            verifySequenceAndClear(callbacks) {
                callbacks.onStateEntry(machine)
                callbacks.onStateEntry(state1)
            }
            machine.processEvent(SwitchEvent)
            machine.activeStates().shouldContainExactly(state2, state21, state212, state22, state222)

            verifySequence {
                callbacks.onStateExit(state1)
                callbacks.onStateEntry(state2)
                callbacks.onStateEntry(state21)
                callbacks.onStateEntry(state212)
                callbacks.onStateEntry(state22)
                callbacks.onStateEntry(state222)
            }
        }

        "transition targets multiple parallel states one same branch (effectively one target state)" {
            val callbacks = mockkCallbacks()

            lateinit var state1: State
            lateinit var state2: State
            lateinit var state21: State
            lateinit var state22: State
            lateinit var state222: State

            val machine = createTestStateMachine(coroutineStarterType) {
                callbacks.listen(this)
                state1 = initialState("state1") {
                    callbacks.listen(this)
                    transitionConditionally<SwitchEvent> {
                        direction = { targetParallelStates(state22, state222) }
                    }
                }
                state2 = state("state2", childMode = ChildMode.PARALLEL) {
                    callbacks.listen(this)
                    state21 = state("state21") {
                        callbacks.listen(this)
                    }
                    state22 = state("state22") {
                        callbacks.listen(this)
                        initialState("state221") {
                            callbacks.listen(this)
                        }
                        state222 = state("state222") {
                            callbacks.listen(this)
                        }
                    }
                }
            }

            verifySequenceAndClear(callbacks) {
                callbacks.onStateEntry(machine)
                callbacks.onStateEntry(state1)
            }
            machine.processEvent(SwitchEvent)
            machine.activeStates().shouldContainExactly(state2, state21, state22, state222)

            verifySequence {
                callbacks.onStateExit(state1)
                callbacks.onStateEntry(state2)
                callbacks.onStateEntry(state21)
                callbacks.onStateEntry(state22)
                callbacks.onStateEntry(state222)
            }
        }

        "transition targets multiple states, negative state amount" {
            lateinit var state1: State

            val machine = createTestStateMachine(coroutineStarterType) {
                state1 = initialState("state1") {
                    transitionConditionally<SwitchEvent> {
                        direction = { targetParallelStates(state1, state1) }
                    }
                }
            }
            shouldThrowWithMessage<IllegalArgumentException>(
                "There should be at least two targetStates, current amount 1, check that you are not using the same state multiple times"
            ) { machine.processEvent(SwitchEvent) }
        }

        "transition targets multiple states negative state targets" {
            lateinit var state1: State
            lateinit var state2: State

            val machine = createTestStateMachine(coroutineStarterType) {
                state1 = initialState("state1") {
                    transitionConditionally<SwitchEvent> {
                        direction = {
                            targetParallelStates(state1, state2)
                        }
                    }
                }
                state2 = state("state2")
            }
            shouldThrowWithMessage<IllegalStateException>(
                "Resolved states does not have common ancestor with ${ChildMode.PARALLEL} child mode. " +
                        "Only children of a state with ${ChildMode.PARALLEL} child mode" +
                        " might be used as effective (resolved) targets here."
            ) { machine.processEvent(SwitchEvent) }
        }

        "complex transition targets multiple parallel states with redirection inside region" {
            val callbacks = mockkCallbacks()

            lateinit var state1: State
            lateinit var state2: State
            lateinit var state21: State
            lateinit var state212: State
            lateinit var state22: State
            lateinit var state222: State
            lateinit var choiceState: State

            val machine = createTestStateMachine(coroutineStarterType) {
                callbacks.listen(this)
                state1 = initialState("state1") {
                    callbacks.listen(this)
                    transitionConditionally<SwitchEvent> {
                        direction = { targetParallelStates(state212, choiceState) }
                    }
                }
                state2 = state("state2", childMode = ChildMode.PARALLEL) {
                    callbacks.listen(this)
                    state21 = state("state21") {
                        callbacks.listen(this)
                        initialState("state211") {
                            callbacks.listen(this)
                        }
                        state212 = state("state212") {
                            callbacks.listen(this)
                        }
                    }
                    state22 = state("state22") {
                        callbacks.listen(this)
                        initialState("state221") {
                            callbacks.listen(this)
                        }
                        state222 = state("state222") {
                            callbacks.listen(this)
                        }
                        choiceState = choiceState { state222 }
                    }
                }
            }

            verifySequenceAndClear(callbacks) {
                callbacks.onStateEntry(machine)
                callbacks.onStateEntry(state1)
            }
            machine.processEvent(SwitchEvent)
            machine.activeStates().shouldContainExactly(state2, state21, state212, state22, state222)

            verifySequence {
                callbacks.onStateExit(state1)
                callbacks.onStateEntry(state2)
                callbacks.onStateEntry(state21)
                callbacks.onStateEntry(state212)
                callbacks.onStateEntry(state22)
                callbacks.onStateEntry(state222)
            }
        }

        "complex transition targets multiple parallel states with redirection outside region" {
            val callbacks = mockkCallbacks()

            lateinit var state1: State
            lateinit var state2: State
            lateinit var state21: State
            lateinit var state212: State
            lateinit var state22: State
            lateinit var state222: State
            lateinit var choiceState: State

            val machine = createTestStateMachine(coroutineStarterType) {
                callbacks.listen(this)
                state1 = initialState("state1") {
                    callbacks.listen(this)
                    transitionConditionally<SwitchEvent> {
                        direction = { targetParallelStates(state212, choiceState) }
                    }
                }
                state2 = state("state2", childMode = ChildMode.PARALLEL) {
                    callbacks.listen(this)
                    state21 = state("state21") {
                        callbacks.listen(this)
                        initialState("state211") {
                            callbacks.listen(this)
                        }
                        state212 = state("state212") {
                            callbacks.listen(this)
                        }
                    }
                    state22 = state("state22") {
                        callbacks.listen(this)
                        initialState("state221") {
                            callbacks.listen(this)
                        }
                        state222 = state("state222") {
                            callbacks.listen(this)
                        }
                    }
                }
                choiceState = choiceState { state222 }
            }

            verifySequenceAndClear(callbacks) {
                callbacks.onStateEntry(machine)
                callbacks.onStateEntry(state1)
            }
            machine.processEvent(SwitchEvent)
            machine.activeStates().shouldContainExactly(state2, state21, state212, state22, state222)

            verifySequence {
                callbacks.onStateExit(state1)
                callbacks.onStateEntry(state2)
                callbacks.onStateEntry(state21)
                callbacks.onStateEntry(state212)
                callbacks.onStateEntry(state22)
                callbacks.onStateEntry(state222)
            }
        }

        "complex transition targets multiple parallel states with nested parallel states" {
            val callbacks = mockkCallbacks()

            lateinit var state1: State
            lateinit var state2: State
            lateinit var state21: State
            lateinit var state212: State
            lateinit var state22: State
            lateinit var state221: State
            lateinit var state222: State
            lateinit var state2222: State

            val machine = createTestStateMachine(coroutineStarterType) {
                callbacks.listen(this)
                state1 = initialState("state1") {
                    callbacks.listen(this)
                    transitionConditionally<SwitchEvent> {
                        direction = { targetParallelStates(state212, state2222) }
                    }
                }
                state2 = state("state2", childMode = ChildMode.PARALLEL) {
                    callbacks.listen(this)
                    state21 = state("state21") {
                        callbacks.listen(this)
                        initialState("state211") {
                            callbacks.listen(this)
                        }
                        state212 = state("state212") {
                            callbacks.listen(this)
                        }
                    }
                    state22 = state("state22", childMode = ChildMode.PARALLEL) {
                        callbacks.listen(this)
                        state221 = state("state221") {
                            callbacks.listen(this)
                        }
                        state222 = state("state222") {
                            callbacks.listen(this)
                            initialState("state2221") {
                                callbacks.listen(this)
                            }
                            state2222 = state("state2222") {
                                callbacks.listen(this)
                            }
                        }
                    }
                }
            }

            verifySequenceAndClear(callbacks) {
                callbacks.onStateEntry(machine)
                callbacks.onStateEntry(state1)
            }
            machine.processEvent(SwitchEvent)
            machine.activeStates().shouldContainExactly(
                state2, state21, state212, state22, state221, state222, state2222
            )

            // todo check trasitionParams args
            verifySequence {
                callbacks.onStateExit(state1)
                callbacks.onStateEntry(state2)
                callbacks.onStateEntry(state21)
                callbacks.onStateEntry(state212)
                callbacks.onStateEntry(state22)
                callbacks.onStateEntry(state221)
                callbacks.onStateEntry(state222)
                callbacks.onStateEntry(state2222)
            }
        }
    }
})