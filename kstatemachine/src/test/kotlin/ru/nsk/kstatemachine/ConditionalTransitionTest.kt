package ru.nsk.kstatemachine

import io.mockk.Called
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.Test

private class ConditionEvent(val data: Boolean) : Event

class ConditionalTransitionTest {
    @Test
    fun conditionalTransitionStay() {
        val callbacks = mockkCallbacks()

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

        verifySequenceAndClear(callbacks) { callbacks.onEntryState(first) }

        machine.processEvent(SwitchEvent)

        verifySequence { callbacks.onTriggeredTransition(SwitchEvent) }
    }

    @Test
    fun conditionalTransitionNoTransition() {
        val callbacks = mockkCallbacks()

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

        verifySequenceAndClear(callbacks) { callbacks.onEntryState(first) }

        machine.processEvent(SwitchEvent)
        verify { callbacks wasNot Called }
    }

    @Test
    fun conditionalTransitionTargetState() {
        val callbacks = mockkCallbacks()

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

        verifySequenceAndClear(callbacks) { callbacks.onEntryState(first) }

        machine.processEvent(SwitchEvent)
        verifySequence {
            callbacks.onTriggeredTransition(SwitchEvent)
            callbacks.onExitState(first)
            callbacks.onEntryState(second)
        }
    }

    @Test
    fun conditionalTransition() {
        val callbacks = mockkCallbacks()

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

        verifySequenceAndClear(callbacks) { callbacks.onEntryState(first) }

        machine.processEvent(SwitchEvent)
        verifySequence {
            callbacks.onTriggeredTransition(SwitchEvent)
            callbacks.onExitState(first)
            callbacks.onEntryState(second)
        }
    }

    @Test
    fun conditionalTransitionByEventData() {
        val callbacks = mockkCallbacks()

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
        verifySequenceAndClear(callbacks) { callbacks.onEntryState(first) }

        machine.processEvent(event)
        verifySequence {
            callbacks.onTriggeredTransition(event)
            callbacks.onExitState(first)
            callbacks.onEntryState(third)
        }
    }
}