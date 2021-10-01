package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

private const val ARGUMENT = 1

class TransitionTest {
    @Test
    fun transitionArgument() {
        val callbacks = mockkCallbacks()

        val second = object : DefaultState("second") {}

        val machine = createStateMachine {
            addState(second) {
                onEntry {
                    callbacks.onEntryState(this)
                    it.transition.argument shouldBe ARGUMENT
                }
            }
            initialState("first") {
                transition<SwitchEvent> {
                    targetState = second
                    onTriggered {
                        it.transition.argument = ARGUMENT
                    }
                }
            }
        }

        machine.processEvent(SwitchEvent)
        verifySequence { callbacks.onEntryState(second) }
    }

    @Test
    fun requireTransition() {
        val state = object : DefaultState() {}

        lateinit var firstTransition: Transition<*>
        lateinit var secondTransition: Transition<*>

        createStateMachine {
            addInitialState(state) {
                firstTransition = transition<FirstEvent>("firstTransition")
                secondTransition = transition<SecondEvent>()
            }
        }

        state.requireTransition("firstTransition") shouldBeSameInstanceAs firstTransition
        state.requireTransition<SecondEvent>() shouldBeSameInstanceAs secondTransition
        shouldThrow<IllegalArgumentException> { state.requireTransition("thirdTransition") }
        shouldThrow<IllegalArgumentException> { state.requireTransition<SwitchEvent>() }
    }

    @Test
    fun transitionDirection() {
        val callbacks = mockkCallbacks()

        lateinit var state1: State
        lateinit var state2: State

        val machine = createStateMachine {
            state1 = initialState("1") {
                onEntry {
                    callbacks.onEntryState(this)
                    it.direction.targetState shouldBeSameInstanceAs this
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

            state2 = state("2") { callbacks.listen(this) }
        }

        verifySequenceAndClear(callbacks) { callbacks.onEntryState(state1) }

        machine.processEvent(SwitchEvent)

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEvent)
            callbacks.onExitState(state1)
            callbacks.onEntryState(state2)
        }
    }

    @Test
    fun topLevelTransition() {
        val callbacks = mockkCallbacks()

        lateinit var state2: State

        val machine = createStateMachine {
            transitionOn<SwitchEvent> {
                targetState = { state2 }
                callbacks.listen(this)
            }

            initialState("state1")
            state2 = state("state2") { callbacks.listen(this) }
        }

        machine.processEvent(SwitchEvent)

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEvent)
            callbacks.onEntryState(state2)
        }
    }

    @Test
    fun transitionToNullTargetState() {
        val callbacks = mockkCallbacks()

        val machine = createStateMachine {
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
}