package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.called
import io.mockk.confirmVerified
import io.mockk.verify
import io.mockk.verifySequence

class FinishingStateMachineTest : StringSpec({

    "finishing state machine" {
        val callbacks = mockkCallbacks()

        lateinit var final: State
        val machine = createStateMachine {
            final = finalState("final") { callbacks.listen(this) }
            setInitialState(final)

            onFinished { callbacks.onFinished(this) }
        }

        verifySequence {
            callbacks.onEntryState(final)
            callbacks.onFinished(machine)
        }
    }

    "restart machine after finish" {
        val callbacks = mockkCallbacks()
        val machine = createStateMachine {
            val final = finalState("final")
            setInitialState(final)
            transition<SwitchEvent> { callbacks.listen(this) }
        }
        machine.isFinished shouldBe true
        machine.processEvent(SwitchEvent)
        confirmVerified(callbacks)
        shouldThrow<IllegalStateException> { machine.start() }
        machine.restart()
    }

    "reenter finished branch" {
        //TODO()
    }

    "finished state machine ignores event" {
        val callbacks = mockkCallbacks()

        lateinit var final: State
        val machine = createStateMachine {
            final = finalState("final") { callbacks.listen(this) }
            setInitialState(final)

            onFinished { callbacks.onFinished(this) }

            transition<SwitchEvent> {
                targetState = final
                callbacks.listen(this)
            }
        }

        verifySequenceAndClear(callbacks) {
            callbacks.onEntryState(final)
            callbacks.onFinished(machine)
        }

        machine.processEvent(SwitchEvent)
        verify { callbacks wasNot called }
    }

    "finished state machine ignores event from deep transition" {
        lateinit var final: State
        val machine = createStateMachine {
            final = finalState("final") {
                initialState("nested state") {
                    transition<SwitchEvent> {
                        onTriggered { error("should not be triggered") }
                    }
                }
            }
            setInitialState(final)
        }

        machine.isFinished shouldBe true
        machine.processEvent(SwitchEvent)
    }

    // FIXME add sample code and readme refs
    // Parallel StateMachine finishes when all direct child states has finished
    "finishing with parallel states" {
        val callbacks = mockkCallbacks()

        lateinit var state1: IState
        lateinit var state11: IState
        lateinit var state12: IState
        lateinit var state2: IState
        val machine = createStateMachine(childMode = ChildMode.PARALLEL) {
            state1 = state("State1", childMode = ChildMode.PARALLEL) {
                state11 = state("State11") {
                    addInitialState(DefaultFinalState("Final state111"))
                    onFinished { callbacks.onFinished(this) }
                }
                state12 = state("State12") {
                    addInitialState(DefaultFinalState("Final state121"))
                    onFinished { callbacks.onFinished(this) }
                }
                onFinished { callbacks.onFinished(this) }
            }
            state2 = state("State2") {
                val finalState22 = finalState("State22")
                initialState {
                    transition<SwitchEvent> { targetState = finalState22 }
                }
                onFinished { callbacks.onFinished(this) }
            }
            onFinished { callbacks.onFinished(this) }
        }

        machine.processEvent(SwitchEvent)

        verifySequence {
            callbacks.onFinished(state11)
            callbacks.onFinished(state12)
            callbacks.onFinished(state1)
            callbacks.onFinished(state2)
            callbacks.onFinished(machine)
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
        createStateMachine(childMode = ChildMode.PARALLEL) {
            state1 = state("State1") {
                addInitialState(DefaultFinalState("Final state11"))
                onFinished { callbacks.onFinished(this) }
            }
            state("State2")

            onFinished { callbacks.onFinished(this) }
        }

        verifySequence {
            callbacks.onFinished(state1)
        }
    }

    "finished parallel branch handles events" {
        val callbacks = mockkCallbacks()
        lateinit var state1: IState
        val machine = createStateMachine(childMode = ChildMode.PARALLEL) {
            state1 = state("state1") {
                addInitialState(DefaultFinalState("finalState11"))
                transition<SwitchEvent> {
                    onTriggered { callbacks.onTriggeredTransition(it.event) }
                }
            }
            state("state2")
        }

        state1.isFinished shouldBe true
        machine.isFinished shouldBe false
        machine.processEvent(SwitchEvent)

        verifySequence { callbacks.onTriggeredTransition(SwitchEvent) }
    }

    "nested finished state does not stop machine and does not ignore events" {
        lateinit var state1: State
        lateinit var state11: State
        lateinit var state112: State
        val machine = createStateMachine {
            state1 = initialState("state1") {
                state11 = initialState("state11") {
                    val final = finalState("finalState111") {
                        transitionOn<SwitchEvent> { targetState = { state112 } }
                    }
                    setInitialState(final)

                    state112 = state("state112")
                }
            }
        }

        state11.isFinished shouldBe true
        state1.isFinished shouldBe false
        machine.isFinished shouldBe false

        machine.processEvent(SwitchEvent)
        state112.isActive shouldBe true
    }

    "nested state finish L2" {
        val callbacks = mockkCallbacks()

        lateinit var initialL1: State
        lateinit var finalL1: State
        lateinit var initialL2: State
        lateinit var finalL2: State

        val machine = createStateMachine {
            logger = StateMachine.Logger { println(it) }

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

            onFinished { callbacks.onFinished(this) }
        }

        verifySequenceAndClear(callbacks) {
            callbacks.onEntryState(initialL1)
            callbacks.onEntryState(initialL2)
        }

        machine.processEvent(SwitchEventL2)

        verifySequenceAndClear(callbacks) {
            callbacks.onTriggeredTransition(SwitchEventL2)
            callbacks.onExitState(initialL2)
            callbacks.onEntryState(finalL2)
            callbacks.onFinished(initialL1)
        }
        initialL1.isFinished shouldBe true

        machine.processEvent(SwitchEventL1)

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEventL1)
            callbacks.onExitState(finalL2)
            callbacks.onExitState(initialL1)
            callbacks.onEntryState(finalL1)
            callbacks.onFinished(machine)
        }
        machine.isFinished shouldBe true
    }

    "composite state isFinished is reset on leaving final state" {
        lateinit var state11: State
        val machine = createStateMachine {
            initialState("state1") {
                state11 = initialState("state11") {
                    val state112 = state("state112")

                    val final = finalState("finalState111") {
                        transition<SwitchEvent>(targetState = state112)
                    }
                    setInitialState(final)
                }
            }
        }

        state11.isFinished shouldBe true
        machine.isFinished shouldBe false

        machine.processEvent(SwitchEvent)
        state11.isFinished shouldBe false
    }

    "composite state isFinished is reset on leaving this state" {
        lateinit var state11: State
        lateinit var state12: State
        val machine = createStateMachine {
            initialState("state1") {
                state11 = initialState("state11") {
                    val final = finalState("finalState111") {
                        transitionOn<SwitchEvent> { targetState = { state12 } }
                    }
                    setInitialState(final)
                }
            }
            state12 = state("state12")
        }

        state11.isFinished shouldBe true
        machine.isFinished shouldBe false

        machine.processEvent(SwitchEvent)
        state11.isFinished shouldBe false
    }
})