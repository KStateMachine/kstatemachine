package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.mockk.verifySequence

class TransitionOverrideTest : StringSpec({
    "override parent transition same event type" { overrideParentTransitionWithEventType<SwitchEvent>() }

    "override parent transition different event type" { overrideParentTransitionWithEventType<Event>() }

    /*
     * It is not possible to override with [noTransition]. Currently, I do not think it is necessary.
     * [stay] should fit such case, see "override with stay()".
     */
    "override with noTransition() negative" {
        val callbacks = mockkCallbacks()

        val machine = overrideWithDirection(callbacks, noTransition())

        machine.processEvent(SwitchEvent)

        verifySequence { callbacks.onTriggeredTransition(SwitchEvent, 2) }
    }

    "override with stay()" {
        val callbacks = mockkCallbacks()

        val machine = overrideWithDirection(callbacks, stay())

        machine.processEvent(SwitchEvent)

        verifySequence { callbacks.onTriggeredTransition(SwitchEvent) }
    }

    "override all events" {
        val callbacks = mockkCallbacks()

        lateinit var state1: State
        lateinit var state2: State

        val machine = createTestStateMachine {
            transitionOn<SwitchEvent> {
                targetState = { state2 }
                onTriggered { callbacks.onTriggeredTransition(it.event, 2) }
            }

            state1 = initialState("state1") {
                // override all events
                transition { callbacks.listen(this) }
                callbacks.listen(this)
            }
            state2 = state("state2") { callbacks.listen(this) }
        }

        verifySequenceAndClear(callbacks) { callbacks.onEntryState(state1) }

        machine.processEvent(SwitchEvent)
        verifySequence { callbacks.onTriggeredTransition(SwitchEvent) }
    }
})

private inline fun <reified E : Event> overrideParentTransitionWithEventType() {
    val callbacks = mockkCallbacks()

    lateinit var state2: State
    lateinit var state3: State

    val machine = createTestStateMachine {
        transitionOn<SwitchEvent> {
            targetState = { state3 }
            onTriggered { callbacks.onTriggeredTransition(it.event, 3) }
        }

        initialState("state1") {
            // overrides parent transition
            transitionOn<E> {
                targetState = { state2 }
                onTriggered { callbacks.onTriggeredTransition(it.event, 2) }
            }
        }
        state2 = state("state2") { callbacks.listen(this) }
        state3 = state("state3") { callbacks.listen(this) }
    }

    machine.processEvent(SwitchEvent)

    verifySequence {
        callbacks.onTriggeredTransition(SwitchEvent, 2)
        callbacks.onEntryState(state2)
    }
}

private fun overrideWithDirection(callbacks: Callbacks, childDirection: TransitionDirection) = createTestStateMachine {
    lateinit var state2: State
    transitionOn<SwitchEvent> {
        targetState = { state2 }
        onTriggered { callbacks.onTriggeredTransition(it.event, 2) }
    }

    initialState("state1") {
        transitionConditionally<SwitchEvent> {
            direction = { childDirection }
            callbacks.listen(this)
        }
    }
    state2 = state("state2")
}
