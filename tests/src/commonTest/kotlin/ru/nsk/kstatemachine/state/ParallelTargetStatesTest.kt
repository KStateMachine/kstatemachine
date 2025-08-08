/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.verifySequence
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.transition.targetParallelStates

class ParallelTargetStatesTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
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
                                onEntry {
                                    it.direction.targetStates shouldContainExactly listOf(state212, state222)
                                    it.direction.targetState shouldBe state212
                                }
                            }
                        }
                        state22 = state("state22") {
                            callbacks.listen(this)
                            initialState("state221") {
                                callbacks.listen(this)
                                onEntry {
                                    it.direction.targetStates shouldContainExactly listOf(state212, state222)
                                    it.direction.targetState shouldBe state222
                                }
                            }
                            state222 = state("state222") {
                                callbacks.listen(this)
                                onEntry {
                                    it.direction.targetStates shouldContainExactly listOf(state212, state222)
                                    it.direction.targetState shouldBe state222
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

            "negative transition targets multiple states, invalid state amount" {
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

            "negative transition targets multiple states, invalid target states" {
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

            "negative, multiple targets for EXCLUSIVE state" {
                lateinit var state221: State
                lateinit var state222: State
                lateinit var state232: State

                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState("state1") {
                        transitionConditionally<SwitchEvent> {
                            direction = { targetParallelStates(state221, state222, state232) }
                        }
                    }
                    state("state2", ChildMode.PARALLEL) {
                        state("state21") {
                            initialState("state211")
                            state("state212")
                        }
                        state("state22") {
                            state221 = initialState("state221")
                            state222 = state("state222")
                        }
                        state("state23") {
                            initialState("state231")
                            state232 = state("state232")
                        }
                    }
                }

                shouldThrow<IllegalStateException> {
                    machine.processEvent(SwitchEvent)
                }.message shouldStartWith "Looks that you have specified multiple targets for exclusive state, which is not correct"
            }
        }
    }
})