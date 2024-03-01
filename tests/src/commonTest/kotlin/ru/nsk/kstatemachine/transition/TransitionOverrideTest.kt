package ru.nsk.kstatemachine.transition

import io.kotest.core.spec.style.StringSpec
import io.mockk.verifySequence
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.processEventBlocking

class TransitionOverrideTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "override parent transition same event type" {
            overrideParentTransitionWithEventType<SwitchEvent>(coroutineStarterType)
        }

        "override parent transition different event type" {
            overrideParentTransitionWithEventType<Event>(coroutineStarterType)
        }

        /*
         * It is not possible to override with [noTransition]. Currently, I do not think it is necessary.
         * [stay] should fit such case, see "override with stay()".
         */
        "override with noTransition() negative" {
            val callbacks = mockkCallbacks()

            val machine = overrideWithDirection(coroutineStarterType, callbacks, noTransition())

            machine.processEventBlocking(SwitchEvent)

            verifySequence { callbacks.onTransitionTriggered(SwitchEvent, 2) }
        }

        "override with stay()" {
            val callbacks = mockkCallbacks()

            val machine = overrideWithDirection(coroutineStarterType, callbacks, stay())

            machine.processEventBlocking(SwitchEvent)

            verifySequence { callbacks.onTransitionTriggered(SwitchEvent) }
        }

        "override all events" {
            val callbacks = mockkCallbacks()

            lateinit var state1: State
            lateinit var state2: State

            val machine = createTestStateMachine(coroutineStarterType) {
                transitionOn<SwitchEvent> {
                    targetState = { state2 }
                    onTriggered { callbacks.onTransitionTriggered(it.event, 2) }
                }

                state1 = initialState("state1") {
                    // override all events
                    transition<Event> { callbacks.listen(this) }
                    callbacks.listen(this)
                }
                state2 = state("state2") { callbacks.listen(this) }
            }

            verifySequenceAndClear(callbacks) { callbacks.onStateEntry(state1) }

            machine.processEventBlocking(SwitchEvent)
            verifySequence { callbacks.onTransitionTriggered(SwitchEvent) }
        }
    }
})

private inline fun <reified E : Event> overrideParentTransitionWithEventType(coroutineStarterType: CoroutineStarterType) {
    val callbacks = mockkCallbacks()

    lateinit var state2: State
    lateinit var state3: State

    val machine = createTestStateMachine(coroutineStarterType) {
        transitionOn<SwitchEvent> {
            targetState = { state3 }
            onTriggered { callbacks.onTransitionTriggered(it.event, 3) }
        }

        initialState("state1") {
            // overrides parent transition
            transitionOn<E> {
                targetState = { state2 }
                onTriggered { callbacks.onTransitionTriggered(it.event, 2) }
            }
        }
        state2 = state("state2") { callbacks.listen(this) }
        state3 = state("state3") { callbacks.listen(this) }
    }

    machine.processEventBlocking(SwitchEvent)

    verifySequence {
        callbacks.onTransitionTriggered(SwitchEvent, 2)
        callbacks.onStateEntry(state2)
    }
}

private fun overrideWithDirection(
    coroutineStarterType: CoroutineStarterType,
    callbacks: Callbacks,
    childDirection: TransitionDirection
) = createTestStateMachine(coroutineStarterType) {
    lateinit var state2: State
    transitionOn<SwitchEvent> {
        targetState = { state2 }
        onTriggered { callbacks.onTransitionTriggered(it.event, 2) }
    }

    initialState("state1") {
        transitionConditionally<SwitchEvent> {
            direction = { childDirection }
            callbacks.listen(this)
        }
    }
    state2 = state("state2")
}
