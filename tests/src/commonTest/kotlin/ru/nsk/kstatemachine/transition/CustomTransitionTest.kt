/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.transition

import io.kotest.core.spec.style.StringSpec
import io.mockk.verifySequence
import ru.nsk.kstatemachine.CoroutineStarterType
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.event.EventMatcher.Companion.isInstanceOf
import ru.nsk.kstatemachine.mockkCallbacks
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.state
import ru.nsk.kstatemachine.statemachine.processEventBlocking
import ru.nsk.kstatemachine.transition.CustomTransitionTestData.CustomEvent
import ru.nsk.kstatemachine.transition.CustomTransitionTestData.CustomTransition

private object CustomTransitionTestData {
    class CustomEvent(val value: Int) : Event

    class CustomTransition(name: String, sourceState: IState, targetState: IState) :
        DefaultTransition<Event>(name, isInstanceOf(), TransitionType.LOCAL, null, sourceState, targetState) {
        override suspend fun isMatchingEvent(event: Event): Boolean {
            return super.isMatchingEvent(event) && event is CustomEvent && event.value == 42
        }
    }
}

/**
 * It is possible to subclass [DefaultTransition] and manually control
 * when it is triggered with [Transition.isMatchingEvent]. Currently, I do not see real use cases to do it,
 * as conditional transitions might be used for the same purpose.
 */
class CustomTransitionTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "custom transition" {
            val callbacks = mockkCallbacks()

            val event = CustomEvent(42)

            val machine = createTestStateMachine(coroutineStarterType) {
                val state2 = state("state2")

                initialState("state1") {
                    val transition = CustomTransition("customTransition", this, state2).apply {
                        onTriggered { callbacks.onTransitionTriggered(it.event) }
                    }
                    addTransition(transition)
                }

            }

            machine.processEventBlocking(event)

            verifySequence { callbacks.onTransitionTriggered(event) }
        }
    }
})