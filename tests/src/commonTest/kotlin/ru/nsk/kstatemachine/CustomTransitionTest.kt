package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.mockk.verifySequence
import ru.nsk.kstatemachine.CustomTransitionTestData.CustomEvent
import ru.nsk.kstatemachine.CustomTransitionTestData.CustomTransition
import ru.nsk.kstatemachine.EventMatcher.Companion.isInstanceOf

private object CustomTransitionTestData {
    class CustomEvent(val value: Int) : Event

    class CustomTransition(name: String, displayName: String, sourceState: IState, targetState: IState) :
        DefaultTransition<Event>(name,  displayName, isInstanceOf(), TransitionType.LOCAL, sourceState, targetState) {
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
                    val transition = CustomTransition("customTransition", "test display name",this, state2).apply {
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