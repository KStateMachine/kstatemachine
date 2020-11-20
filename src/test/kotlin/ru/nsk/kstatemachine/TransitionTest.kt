package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import io.kotest.assertions.throwables.shouldThrow
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.sameInstance
import org.junit.jupiter.api.Test

const val ARG = 1

class TransitionArgumentTest {
    @Test
    fun transitionArgument() {
        val callbacks = mock<Callbacks>()

        val second = object : State("second") {}

        val stateMachine = createStateMachine {
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

        stateMachine.processEvent(SwitchEvent)
        then(callbacks).should().onEntryState(second)
    }

    @Test
    fun requireTransition() {
        val state = object : State() {}

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
}