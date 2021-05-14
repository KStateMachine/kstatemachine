package ru.nsk.kstatemachine

import io.mockk.verifySequence
import org.junit.jupiter.api.Test

class TransitionOverrideTest {
    @Test
    fun overrideParentTransitionSameEventType() = overrideParentTransitionWithEventType<SwitchEvent>()

    @Test
    fun overrideParentTransitionDifferentEventType() = overrideParentTransitionWithEventType<Event>()

    /**
     * It is not possible to override with [noTransition]. Currently I do not think it is necessary.
     * [stay] should fit such case, see [overrideWithStay].
     */
    @Test
    fun overrideWithNoTransitionNegative() {
        val callbacks = mockkCallbacks()

        val machine = overrideWithDirection(callbacks, noTransition())

        machine.processEvent(SwitchEvent)

        verifySequence { callbacks.onTriggeredTransition(SwitchEvent, 2) }
    }

    @Test
    fun overrideWithStay() {
        val callbacks = mockkCallbacks()

        val machine = overrideWithDirection(callbacks, stay())

        machine.processEvent(SwitchEvent)

        verifySequence { callbacks.onTriggeredTransition(SwitchEvent) }
    }

    @Test
    fun overrideAllEvents() {
        val callbacks = mockkCallbacks()

        lateinit var state1: State
        lateinit var state2: State

        val machine = createStateMachine {
            transitionOn<SwitchEvent> {
                targetState = { state2 }
                onTriggered { callbacks.onTriggeredTransition(it.event, 2) }
            }

            state1 = initialState("state1") {
                // override all events
                transition<Event> { callbacks.listen(this) }
                callbacks.listen(this)
            }
            state2 = state("state2") { callbacks.listen(this) }
        }

        verifySequenceAndClear(callbacks) { callbacks.onEntryState(state1) }

        machine.processEvent(SwitchEvent)
        verifySequence { callbacks.onTriggeredTransition(SwitchEvent) }
    }
}

private inline fun <reified E : Event> overrideParentTransitionWithEventType() {
    val callbacks = mockkCallbacks()

    lateinit var state2: State
    lateinit var state3: State

    val machine = createStateMachine {
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

private fun overrideWithDirection(callbacks: Callbacks, childDirection: TransitionDirection) = createStateMachine {
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
