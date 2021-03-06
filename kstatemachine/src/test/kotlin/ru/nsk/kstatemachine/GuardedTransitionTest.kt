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
                    callbacks.listen(this)
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
    fun guardedTransitionOnWithLateinitState() {
        val callbacks = mock<Callbacks>()

        var value = "value1";

        val machine = createStateMachine {
            lateinit var second: State

            initialState("first") {
                transitionOn<SwitchEvent> {
                    guard = { value == "value2" }
                    targetState = { second }
                    callbacks.listen(this)
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

    @Test
    fun guardedTransitionSameEvent() {
        val callbacks = mock<Callbacks>()

        lateinit var state1: State
        lateinit var state2: State
        lateinit var state3: State

        val machine = createStateMachine {
            state1 = initialState("state1") {
                callbacks.listen(this)

                transitionOn<SwitchEvent> {
                    guard = { false }
                    targetState = { state2 }
                    callbacks.listen(this)
                }

                transitionOn<SwitchEvent> {
                    guard = { true }
                    targetState = { state3 }
                    callbacks.listen(this)
                }
            }

            state2 = state("state2") { callbacks.listen(this) }
            state3 = state("state3") { callbacks.listen(this) }
        }

        then(callbacks).should().onEntryState(state1)

        machine.processEvent(SwitchEvent)

        then(callbacks).should().onTriggeredTransition(SwitchEvent)
        then(callbacks).should().onExitState(state1)
        then(callbacks).should().onEntryState(state3)
        then(callbacks).shouldHaveNoMoreInteractions()
    }
}