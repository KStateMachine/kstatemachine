package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.confirmVerified
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.fail
import ru.nsk.kstatemachine.ProcessingResult.PROCESSED

class TransitionTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "transition add after machine start" {
            lateinit var state1: State
            createTestStateMachine(coroutineStarterType) {
                state1 = initialState()
            }
            shouldThrow<IllegalStateException> { state1.transition<SwitchEvent>() }
        }

        "transition direction" {
            val callbacks = mockkCallbacks()

            lateinit var state1: State
            lateinit var state2: State

            val machine = createTestStateMachine(coroutineStarterType) {
                state1 = initialState("state1") {
                    onEntry {
                        callbacks.onStateEntry(this)
                        it.transition.sourceState shouldBeSameInstanceAs this@createTestStateMachine
                        it.direction.targetState shouldBeSameInstanceAs this@createTestStateMachine
                    }

                    onExit {
                        if (it.direction.targetState === state2)
                            callbacks.onStateExit(this)
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

            verifySequenceAndClear(callbacks) { callbacks.onStateEntry(state1) }

            machine.processEventBlocking(SwitchEvent) shouldBe PROCESSED

            verifySequence {
                callbacks.onTransitionTriggered(SwitchEvent)
                callbacks.onStateExit(state1)
                callbacks.onStateEntry(state2)
            }
        }

        "top level transition" {
            val callbacks = mockkCallbacks()

            lateinit var state2: State

            val machine = createTestStateMachine(coroutineStarterType) {
                transitionOn<SwitchEvent> {
                    targetState = { state2 }
                    callbacks.listen(this)
                }

                initialState("state1")
                state2 = state("state2") { callbacks.listen(this) }
            }

            machine.processEventBlocking(SwitchEvent) shouldBe PROCESSED

            verifySequence {
                callbacks.onTransitionTriggered(SwitchEvent)
                callbacks.onStateEntry(state2)
            }
        }

        "transition to null target state" {
            val callbacks = mockkCallbacks()

            val machine = createTestStateMachine(coroutineStarterType) {
                initialState("initial") {
                    transition<SwitchEvent> {
                        targetState = null
                        callbacks.listen(this)
                    }
                }
            }

            machine.processEventBlocking(SwitchEvent)
            verify { callbacks.onTransitionTriggered(SwitchEvent) }
        }

        "transition with shortcut method" {
            lateinit var finalState: FinalState

            val machine = createTestStateMachine(coroutineStarterType) {
                finalState = finalState()
                initialState {
                    transition<SwitchEvent>("transition1", finalState)
                }
            }

            machine.processEventBlocking(SwitchEvent)
            finalState.isActive shouldBe true
        }

        "transition to free state, negative" {
            val freeState = DefaultState()
            val machine = createTestStateMachine(coroutineStarterType, name = "outer") {
                initialState {
                    transitionOn<SwitchEvent> { targetState = { freeState } } // invalid
                }
            }
            shouldThrow<IllegalStateException> { machine.processEventBlocking(SwitchEvent) }
        }

        "transition to non machine state, negative" {
            val otherMachine = createTestStateMachine(coroutineStarterType) {
                initialState()
            }
            val machine = createTestStateMachine(coroutineStarterType, name = "outer") {
                initialState {
                    transitionOn<SwitchEvent> { targetState = { otherMachine } } // invalid
                }
            }

            shouldThrow<IllegalStateException> { machine.processEventBlocking(SwitchEvent) }
        }

        "multiple matching transitions negative" {
            val machine = createTestStateMachine(coroutineStarterType) {
                transition<SwitchEvent>()
                transition<SwitchEvent>()
                initialState()
            }

            shouldThrow<IllegalStateException> { machine.processEventBlocking(SwitchEvent) }
        }

        "multiple matching transitions" {
            val machine = createTestStateMachine(coroutineStarterType, doNotThrowOnMultipleTransitionsMatch = true) {
                transition<SwitchEvent>()
                transition<SwitchEvent>()
                initialState()
            }

            machine.processEventBlocking(SwitchEvent)
        }

        "parallel multiple matching transitions negative" {
            val machine = createTestStateMachine(coroutineStarterType, childMode = ChildMode.PARALLEL) {
                state { transition<SwitchEvent>() }
                state { transition<SwitchEvent>() }
            }

            shouldThrow<IllegalStateException> { machine.processEventBlocking(SwitchEvent) }
        }

        "parallel multiple matching transitions" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                childMode = ChildMode.PARALLEL,
                doNotThrowOnMultipleTransitionsMatch = true
            ) {
                state { transition<SwitchEvent>() }
                state { transition<SwitchEvent>() }
            }

            machine.processEventBlocking(SwitchEvent)
        }

        "self-targeted transition does not trigger reentering states" {
            val callbacks = mockkCallbacks()
            lateinit var state1: State
            lateinit var state11: State

            val machine = createTestStateMachine(coroutineStarterType) {
                state1 = initialState("state1") {
                    transition<SwitchEvent> { targetState = this@initialState }
                    callbacks.listen(this)
                    state11 = initialState("state11") {
                        callbacks.listen(this)
                    }
                }
            }

            verifySequenceAndClear(callbacks) {
                callbacks.onStateEntry(state1)
                callbacks.onStateEntry(state11)
            }
            machine.processEvent(SwitchEvent)

            confirmVerified(callbacks)
        }
    }
})