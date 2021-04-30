package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import org.junit.jupiter.api.Test

private class ConditionEvent(val data: Boolean) : Event

class ConditionalTransitionTest {
    @Test
    fun conditionalTransitionStay() {
        val callbacks = mock<Callbacks>()

        val first = object : DefaultState("first") {}

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

        val first = object : DefaultState("first") {}

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

        val first = object : DefaultState("first") {}
        val second = object : DefaultState("second") {}

        val machine = createStateMachine {
            addInitialState(first) {
                callbacks.listen(this)

                transitionConditionally<SwitchEvent> {
                    direction = { targetState(second) }
                    callbacks.listen(this)
                }
            }
            addState(second) { callbacks.listen(this) }
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

        val first = object : DefaultState("first") {}
        val second = object : DefaultState("second") {}

        val machine = createStateMachine {
            addInitialState(first) {
                callbacks.listen(this)

                transitionConditionally<SwitchEvent> {
                    direction = { targetState(second) }
                    callbacks.listen(this)
                }
            }
            addState(second) { callbacks.listen(this) }
        }

        then(callbacks).should().onEntryState(first)

        machine.processEvent(SwitchEvent)
        then(callbacks).should().onTriggeredTransition(SwitchEvent)
        then(callbacks).should().onExitState(first)
        then(callbacks).should().onEntryState(second)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun conditionalTransitionByEventData() {
        val callbacks = mock<Callbacks>()

        val first = object : DefaultState("first") {}
        val second = object : DefaultState("second") {}
        val third = object : DefaultState("third") {}

        val machine = createStateMachine {
            addInitialState(first) {
                callbacks.listen(this)

                transitionConditionally<ConditionEvent> {
                    direction = { if (it.data) targetState(second) else targetState(third) }
                    callbacks.listen(this)
                }
            }
            addState(second) { callbacks.listen(this) }
            addState(third) { callbacks.listen(this) }
        }

        val event = ConditionEvent(false)
        then(callbacks).should().onEntryState(first)

        machine.processEvent(event)
        then(callbacks).should().onTriggeredTransition(event)
        then(callbacks).should().onExitState(first)
        then(callbacks).should().onEntryState(third)
        then(callbacks).shouldHaveNoMoreInteractions()
    }
}