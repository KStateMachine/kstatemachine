package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import org.junit.jupiter.api.Test

class GuardedTransitionTest {
    @Test
    fun guardedTransition() {
        val callbacks = mock<Callbacks>()

        var value = "value1";

        val machine = createStateMachine {
            val second = state("second")

            initialState("first") {
                transition<SwitchEvent> {
                    guard = { value == "value2" }
                    targetState = second
                    onTriggered { callbacks.onTriggeredTransition(it.event) }
                }
            }
        }

        machine.processEvent(SwitchEvent)
        then(callbacks).shouldHaveZeroInteractions()
        value = "value2"
        machine.processEvent(SwitchEvent)
        then(callbacks).should().onTriggeredTransition(SwitchEvent)
    }

    @Test
    fun guardedTransitionToWithLateinitState() {
        val callbacks = mock<Callbacks>()

        var value = "value1";

        val machine = createStateMachine {
            lateinit var second: State

            initialState("first") {
                transitionTo<SwitchEvent> {
                    guard = { value == "value2" }
                    targetState = { second }
                    onTriggered { callbacks.onTriggeredTransition(it.event) }
                }
            }

            second = state("second")
        }

        machine.processEvent(SwitchEvent)
        then(callbacks).shouldHaveZeroInteractions()
        value = "value2"
        machine.processEvent(SwitchEvent)
        then(callbacks).should().onTriggeredTransition(SwitchEvent)
    }
}