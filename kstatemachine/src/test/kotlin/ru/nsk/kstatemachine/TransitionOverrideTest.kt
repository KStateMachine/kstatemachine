package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
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
        val callbacks = mock<Callbacks>()

        val machine = overrideWithDirection(callbacks, noTransition())

        machine.processEvent(SwitchEvent)

        then(callbacks).should().onTriggeredTransition(SwitchEvent, 2)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun overrideWithStay() {
        val callbacks = mock<Callbacks>()

        val machine = overrideWithDirection(callbacks, stay())

        machine.processEvent(SwitchEvent)

        then(callbacks).should().onTriggeredTransition(SwitchEvent)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun overrideAllEvents() {
        val callbacks = mock<Callbacks>()

        lateinit var state1: UnitState
        lateinit var state2: UnitState

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

        then(callbacks).should().onEntryState(state1)
        machine.processEvent(SwitchEvent)
        then(callbacks).should().onTriggeredTransition(SwitchEvent)
        then(callbacks).shouldHaveNoMoreInteractions()
    }
}

private inline fun <reified E : Event> overrideParentTransitionWithEventType() {
    val callbacks = mock<Callbacks>()

    lateinit var state2: UnitState
    lateinit var state3: UnitState

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

    then(callbacks).should().onTriggeredTransition(SwitchEvent, 2)
    then(callbacks).should().onEntryState(state2)
    then(callbacks).shouldHaveNoMoreInteractions()
}

private fun overrideWithDirection(callbacks: Callbacks, childDirection: TransitionDirection) = createStateMachine {
    lateinit var state2: UnitState
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
