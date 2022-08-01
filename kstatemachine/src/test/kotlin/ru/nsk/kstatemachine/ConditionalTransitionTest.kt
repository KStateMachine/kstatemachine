package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.mockk.Called
import io.mockk.verify
import io.mockk.verifySequence

private class ConditionEvent(val data: Boolean) : Event

class ConditionalTransitionTest : StringSpec({
    "conditional transition stay()" {
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

    "conditional transition noTransition()" {
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
            onTransition { callbacks.onTriggeredTransition(it.event) }
        }

        verifySequenceAndClear(callbacks) { callbacks.onEntryState(first) }

        machine.processEvent(SwitchEvent)
        verify { callbacks wasNot Called }
    }

    "conditional transition targetState()" {
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

    "conditional transition" {
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

    "conditional transition by event data" {
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

    "conditional transition by argument" {
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
})