package ru.nsk.kstatemachine.statemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.called
import io.mockk.confirmVerified
import io.mockk.verify
import io.mockk.verifySequence
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.transition.onTriggered

class FinishingStateMachineTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "finishing state machine" {
            val callbacks = mockkCallbacks()

            lateinit var final: State
            val machine = createTestStateMachine(coroutineStarterType) {
                final = initialFinalState("final") { callbacks.listen(this) }

                onFinished { callbacks.onStateFinished(this) }
                onStateFinished { state, _ -> callbacks.onStateFinished(state) }
            }

            verifySequence {
                callbacks.onStateEntry(final)
                callbacks.onStateFinished(machine)
                callbacks.onStateFinished(machine)
            }
        }

        "restart machine after finish" {
            val callbacks = mockkCallbacks()
            val machine = createTestStateMachine(coroutineStarterType) {
                initialFinalState("final")
                transition<SwitchEvent> { callbacks.listen(this) }
            }
            machine.isFinished shouldBe true
            machine.processEventBlocking(SwitchEvent)
            confirmVerified(callbacks)
            shouldThrow<IllegalStateException> { machine.startBlocking() }
            machine.restartBlocking()
        }

        "finished state machine ignores event" {
            val callbacks = mockkCallbacks()

            lateinit var final: State
            val machine = createTestStateMachine(coroutineStarterType) {
                final = initialFinalState("final") { callbacks.listen(this) }

                onFinished { callbacks.onStateFinished(this) }

                transition<SwitchEvent> {
                    targetState = final
                    callbacks.listen(this)
                }
            }

            verifySequenceAndClear(callbacks) {
                callbacks.onStateEntry(final)
                callbacks.onStateFinished(machine)
            }

            machine.processEventBlocking(SwitchEvent)
            verify { callbacks wasNot called }
        }

        "finished state machine ignores event from deep transition" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialFinalState("final") {
                    initialState("nested state") {
                        transition<SwitchEvent> {
                            onTriggered { error("should not be triggered") }
                        }
                    }
                }
            }

            machine.isFinished shouldBe true
            machine.processEventBlocking(SwitchEvent)
        }

        // Parallel StateMachine finishes when all direct child states has finished
        "finishing with parallel states" {
            val callbacks = mockkCallbacks()

            lateinit var state1: IState
            lateinit var state11: IState
            lateinit var state12: IState
            lateinit var state2: IState
            val machine = createTestStateMachine(coroutineStarterType, childMode = ChildMode.PARALLEL) {
                state1 = state("State1", childMode = ChildMode.PARALLEL) {
                    state11 = state("State11") {
                        addInitialState(DefaultFinalState("Final state111"))
                        onFinished { callbacks.onStateFinished(this) }
                    }
                    state12 = state("State12") {
                        addInitialState(DefaultFinalState("Final state121"))
                        onFinished { callbacks.onStateFinished(this) }
                    }
                    onFinished { callbacks.onStateFinished(this) }
                }
                state2 = state("State2") {
                    val finalState22 = finalState("State22")
                    initialState {
                        transition<SwitchEvent> { targetState = finalState22 }
                    }
                    onFinished { callbacks.onStateFinished(this) }
                }
                onFinished { callbacks.onStateFinished(this) }
            }

            machine.processEventBlocking(SwitchEvent)

            verifySequence {
                callbacks.onStateFinished(state11)
                callbacks.onStateFinished(state12)
                callbacks.onStateFinished(state1)
                callbacks.onStateFinished(state2)
                callbacks.onStateFinished(machine)
            }

            machine.isFinished shouldBe true
            state1.isFinished shouldBe true
            state11.isFinished shouldBe true
            state12.isFinished shouldBe true
            state2.isFinished shouldBe true
        }

        "finishing with parallel states negative" {
            val callbacks = mockkCallbacks()

            lateinit var state1: IState
            createTestStateMachine(coroutineStarterType, childMode = ChildMode.PARALLEL) {
                state1 = state("State1") {
                    addInitialState(DefaultFinalState("Final state11"))
                    onFinished { callbacks.onStateFinished(this) }
                }
                state("State2")

                onFinished { callbacks.onStateFinished(this) }
            }

            verifySequence {
                callbacks.onStateFinished(state1)
            }
        }

        "finished parallel branch handles events" {
            val callbacks = mockkCallbacks()
            lateinit var state1: IState
            val machine = createTestStateMachine(coroutineStarterType, childMode = ChildMode.PARALLEL) {
                state1 = state("state1") {
                    addInitialState(DefaultFinalState("finalState11"))
                    transition<SwitchEvent> {
                        onTriggered { callbacks.onTransitionTriggered(it.event) }
                    }
                }
                state("state2")
            }

            state1.isFinished shouldBe true
            machine.isFinished shouldBe false
            machine.processEventBlocking(SwitchEvent)

            verifySequence { callbacks.onTransitionTriggered(SwitchEvent) }
        }

        "nested finished state does not stop machine and does not ignore events" {
            lateinit var state1: State
            lateinit var state11: State
            lateinit var state112: State
            val machine = createTestStateMachine(coroutineStarterType) {
                state1 = initialState("state1") {
                    state11 = initialState("state11") {
                        initialFinalState("finalState111") {
                            transitionOn<SwitchEvent> { targetState = { state112 } }
                        }
                        state112 = state("state112")
                    }
                }
            }

            state11.isFinished shouldBe true
            state1.isFinished shouldBe false
            machine.isFinished shouldBe false

            machine.processEventBlocking(SwitchEvent)
            state112.isActive shouldBe true
        }

        "nested state finish L2" {
            val callbacks = mockkCallbacks()

            lateinit var initialL1: State
            lateinit var finalL1: State
            lateinit var initialL2: State
            lateinit var finalL2: State

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it()) }

                finalL1 = finalState("finalL1") {
                    callbacks.listen(this)
                }

                initialL1 = initialState("initialL1") {
                    callbacks.listen(this)

                    transition<SwitchEventL1> {
                        targetState = finalL1
                        callbacks.listen(this)
                    }

                    finalL2 = finalState("finalL2") {
                        callbacks.listen(this)
                    }

                    initialL2 = initialState("initialL2") {
                        callbacks.listen(this)

                        transition<SwitchEventL2> {
                            targetState = finalL2
                            callbacks.listen(this)
                        }
                    }
                }

                onFinished { callbacks.onStateFinished(this) }
            }

            verifySequenceAndClear(callbacks) {
                callbacks.onStateEntry(initialL1)
                callbacks.onStateEntry(initialL2)
            }

            machine.processEventBlocking(SwitchEventL2)

            verifySequenceAndClear(callbacks) {
                callbacks.onTransitionTriggered(SwitchEventL2)
                callbacks.onStateExit(initialL2)
                callbacks.onStateEntry(finalL2)
                callbacks.onStateFinished(initialL1)
            }
            initialL1.isFinished shouldBe true

            machine.processEventBlocking(SwitchEventL1)

            verifySequence {
                callbacks.onTransitionTriggered(SwitchEventL1)
                callbacks.onStateExit(finalL2)
                callbacks.onStateExit(initialL1)
                callbacks.onStateEntry(finalL1)
                callbacks.onStateFinished(machine)
            }
            machine.isFinished shouldBe true
        }
    }
})