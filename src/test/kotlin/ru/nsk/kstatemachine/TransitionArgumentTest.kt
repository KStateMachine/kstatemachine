package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
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
            val first = state("first") {
                transition<SwitchEvent> {
                    targetState = second
                    onTriggered {
                        it.transition.argument = ARG
                    }
                }
            }

            setInitialState(first)
        }

        stateMachine.processEvent(SwitchEvent)
        then(callbacks).should().onEntryState(second)
    }
}