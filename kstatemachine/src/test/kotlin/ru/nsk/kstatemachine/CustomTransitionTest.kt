package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import org.junit.jupiter.api.Test
import ru.nsk.kstatemachine.EventMatcher.Companion.isInstanceOf

private class CustomEvent(val value: Int) : UnitEvent()

class CustomTransition(name: String, sourceState: State, targetState: State) :
    DefaultTransition<Event>(name, isInstanceOf(), sourceState, targetState) {
    override fun isMatchingEvent(event: Event): Boolean {
        return super.isMatchingEvent(event) && event is CustomEvent && event.value == 42
    }
}

/**
 * This is possible to subclass [DefaultTransition] and manually control
 * when it is triggered with [Transition.isMatchingEvent]. Currently I do not see real use cases to do it,
 * as conditional transitions might be used for the same purpose.
 */
class CustomTransitionTest {
    @Test
    fun customTransition() {
        val callbacks = mock<Callbacks>()

        val event = CustomEvent(42)

        val machine = createStateMachine {
            val state2 = state("state2")

            initialState("state1") {
                val transition = CustomTransition("customTransition", this, state2).apply {
                    onTriggered { callbacks.onTriggeredTransition(it.event) }
                }
                addTransition(transition)
            }

        }

        machine.processEvent(event)

        then(callbacks).should().onTriggeredTransition(event)
    }
}