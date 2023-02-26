package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.fail
import ru.nsk.kstatemachine.ProcessingResult.PROCESSED

class TransitionTest : StringSpec({
    "transition add after machine start" {
        lateinit var state1: State
        createTestStateMachine {
            state1 = initialState()
        }
        shouldThrow<IllegalStateException> { state1.transition<SwitchEvent>() }
    }

    "transition direction" {
        val callbacks = mockkCallbacks()

        lateinit var state1: State
        lateinit var state2: State

        val machine = createTestStateMachine {
            state1 = initialState("state1") {
                onEntry {
                    callbacks.onEntryState(this)
                    it.transition.sourceState shouldBeSameInstanceAs this@createTestStateMachine
                    it.direction.targetState shouldBeSameInstanceAs this@createTestStateMachine
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

            state2 = state("state2") { callbacks.listen(this) }
        }

        verifySequenceAndClear(callbacks) { callbacks.onEntryState(state1) }

        machine.processEvent(SwitchEvent) shouldBe PROCESSED

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEvent)
            callbacks.onExitState(state1)
            callbacks.onEntryState(state2)
        }
    }

    "top level transition" {
        val callbacks = mockkCallbacks()

        lateinit var state2: State

        val machine = createTestStateMachine {
            transitionOn<SwitchEvent> {
                targetState = { state2 }
                callbacks.listen(this)
            }

            initialState("state1")
            state2 = state("state2") { callbacks.listen(this) }
        }

        machine.processEvent(SwitchEvent) shouldBe PROCESSED

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEvent)
            callbacks.onEntryState(state2)
        }
    }

    "transition to null target state" {
        val callbacks = mockkCallbacks()

        val machine = createTestStateMachine {
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

        val machine = createTestStateMachine {
            finalState = finalState()
            initialState {
                transition<SwitchEvent>("transition1", finalState)
            }
        }

        machine.processEvent(SwitchEvent)
        finalState.isActive shouldBe true
    }

    "transition to free state, negative" {
        val freeState = DefaultState()
        val machine = createTestStateMachine("outer") {
            initialState {
                transitionOn<SwitchEvent> { targetState = { freeState } } // invalid
            }
        }
        shouldThrow<IllegalStateException> { machine.processEvent(SwitchEvent) }
    }

    "transition to non machine state, negative" {
        val otherMachine = createTestStateMachine {
            initialState()
        }
        val machine = createTestStateMachine("outer") {
            initialState {
                transitionOn<SwitchEvent> { targetState = { otherMachine } } // invalid
            }
        }

        shouldThrow<IllegalStateException> { machine.processEvent(SwitchEvent) }
    }

    "multiple matching transitions negative" {
        val machine = createTestStateMachine {
            transition<SwitchEvent>()
            transition<SwitchEvent>()
            initialState()
        }

        shouldThrow<IllegalStateException> { machine.processEvent(SwitchEvent) }
    }

    "multiple matching transitions" {
        val machine = createTestStateMachine(doNotThrowOnMultipleTransitionsMatch = true) {
            transition<SwitchEvent>()
            transition<SwitchEvent>()
            initialState()
        }

        machine.processEvent(SwitchEvent)
    }

    "parallel multiple matching transitions negative" {
        val machine = createTestStateMachine(childMode = ChildMode.PARALLEL) {
            state {
                transition<SwitchEvent>()
            }
            state {
                transition<SwitchEvent>()
            }
        }

        shouldThrow<IllegalStateException> { machine.processEvent(SwitchEvent) }
    }

    "parallel multiple matching transitions" {
        val machine = createTestStateMachine(childMode = ChildMode.PARALLEL, doNotThrowOnMultipleTransitionsMatch = true) {
            state {
                transition<SwitchEvent>()
            }
            state {
                transition<SwitchEvent>()
            }
        }

        machine.processEvent(SwitchEvent)
    }
})