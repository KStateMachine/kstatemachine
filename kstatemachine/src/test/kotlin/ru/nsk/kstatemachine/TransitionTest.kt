package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import io.kotest.assertions.throwables.shouldThrow
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.sameInstance
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

const val ARG = 1

class TransitionTest {
    @Test
    fun transitionArgument() {
        val callbacks = mock<Callbacks>()

        val second = object : DefaultState("second") {}

        val machine = createStateMachine {
            addState(second) {
                onEntry {
                    callbacks.onEntryState(this)
                    assertThat(it.transition.argument, equalTo(ARG))
                }
            }
            initialState("first") {
                transition<SwitchEvent> {
                    targetState = second
                    onTriggered {
                        it.transition.argument = ARG
                    }
                }
            }
        }

        machine.processEvent(SwitchEvent)
        then(callbacks).should().onEntryState(second)
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

        assertThat(state.requireTransition("firstTransition"), sameInstance(firstTransition))
        assertThat(state.requireTransition<SecondEvent>(), sameInstance(secondTransition))
        shouldThrow<IllegalArgumentException> { state.requireTransition("thirdTransition") }
        shouldThrow<IllegalArgumentException> { state.requireTransition<SwitchEvent>() }
    }

    @Test
    fun transitionDirection() {
        val callbacks = mock<Callbacks>()

        lateinit var state1: State
        lateinit var state2: State

        val machine = createStateMachine {
            state1 = initialState("1") {
                onEntry {
                    callbacks.onEntryState(this)
                    assertThat(it.direction.targetState, sameInstance(this))
                }

                onExit {
                    if (it.direction.targetState == state2)
                        callbacks.onExitState(this)
                    else
                        fail("incorrect direction ${it.direction}")
                }

                transitionTo<SwitchEvent> {
                    targetState = { state2 }
                    callbacks.listen(this)
                }
            }

            state2 = state("2")
        }

        then(callbacks).should().onEntryState(state1)

        machine.processEvent(SwitchEvent)

        then(callbacks).should().onTriggeredTransition(SwitchEvent)
        then(callbacks).should().onExitState(state1)
        then(callbacks).should().onEntryState(state1)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun topLevelTransition() {
        val callbacks = mock<Callbacks>()

        lateinit var state2: State

        val machine = createStateMachine {
            transitionTo<SwitchEvent> {
                targetState = { state2 }
                callbacks.listen(this)
            }

            initialState("state1")
            state2 = state("state2") { callbacks.listen(this) }
        }

        machine.processEvent(SwitchEvent)

        then(callbacks).should().onTriggeredTransition(SwitchEvent)
        then(callbacks).should().onEntryState(state2)
    }
}