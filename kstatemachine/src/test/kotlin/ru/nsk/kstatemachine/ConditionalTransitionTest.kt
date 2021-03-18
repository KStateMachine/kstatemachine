package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import org.junit.jupiter.api.Test

class ConditionalTransitionTest {
    @Test
    fun conditionalTransitionStay() {
        val callbacks = mock<Callbacks>()

        val first = object : DefaultUnitState("first") {}

        val machine = createStateMachine {
            addInitialState(first) {
                callbacks.listen(this)

                transitionConditionally<SwitchEvent> {
                    direction = { stay() }
                    callbacks.listen(this)
                }
            }
        }

        then(callbacks).should().onEntryState(first)
        machine.processEvent(SwitchEvent)

        then(callbacks).should().onTriggeredTransition(SwitchEvent)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun conditionalTransitionNoTransition() {
        val callbacks = mock<Callbacks>()

        val first = object : DefaultUnitState("first") {}

        val machine = createStateMachine {
            addInitialState(first) {
                callbacks.listen(this)

                transitionConditionally<SwitchEvent> {
                    direction = { noTransition() }
                    callbacks.listen(this)
                }
            }
            onTransition { _, _, event, _ ->
                callbacks.onTriggeredTransition(event)
            }
        }

        then(callbacks).should().onEntryState(first)

        machine.processEvent(SwitchEvent)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun conditionalTransitionTargetState() {
        val callbacks = mock<Callbacks>()

        val first = object : DefaultUnitState("first") {}
        val second = object : DefaultUnitState("second") {}

        val machine = createStateMachine {
            addInitialState(first) {
                callbacks.listen(this)

                transitionConditionally<SwitchEvent> {
                    direction = { targetState(second) }
                    callbacks.listen(this)
                }
            }
            addState(second) {
                callbacks.listen(this)
            }
        }

        then(callbacks).should().onEntryState(first)

        machine.processEvent(SwitchEvent)
        then(callbacks).should().onTriggeredTransition(SwitchEvent)
        then(callbacks).should().onExitState(first)
        then(callbacks).should().onEntryState(second)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun conditionalTransition() {
        val callbacks = mock<Callbacks>()

        val first = object : DefaultUnitState("first") {}
        val second = object : DefaultUnitState("second") {}

        val machine = createStateMachine {
            addInitialState(first) {
                callbacks.listen(this)

                transitionConditionally<SwitchEvent> {
                    direction = { targetState(second) }
                    callbacks.listen(this)
                }
            }
            addState(second) {
                callbacks.listen(this)
            }
        }

        then(callbacks).should().onEntryState(first)

        machine.processEvent(SwitchEvent)
        then(callbacks).should().onTriggeredTransition(SwitchEvent)
        then(callbacks).should().onExitState(first)
        then(callbacks).should().onEntryState(second)
        then(callbacks).shouldHaveNoMoreInteractions()
    }
}