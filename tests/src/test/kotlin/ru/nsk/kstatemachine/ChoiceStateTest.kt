package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.mockk.verifySequence
import ru.nsk.kstatemachine.ChoiceStateTestData.State1
import ru.nsk.kstatemachine.ChoiceStateTestData.State2

private object ChoiceStateTestData {
    object State1 : DefaultState()
    object State2 : DefaultState()
}

class ChoiceStateTest : StringSpec({
    CoroutineStarterType.values().forEach { coroutineStarterType ->
        "redirecting choice state" {
            val callbacks = mockkCallbacks()

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it()) }

                val choice = choiceState("choice") {
                    log { "$event $argument" }
                    State2
                }

                addInitialState(State1) {
                    transition<SwitchEvent> { targetState = choice }
                }
                addState(State2) { callbacks.listen(this) }
                onTransition { log { it.toString() } }
            }

            machine.processEventBlocking(SwitchEvent, false)

            verifySequence { callbacks.onEntryState(State2) }
        }

        "redirecting choice states chain" {
            val callbacks = mockkCallbacks()

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it()) }

                val choice2 = choiceState("choice2") { State2 }
                val choice1 = choiceState("choice1") { choice2 }

                addInitialState(State1) {
                    transition<SwitchEvent> { targetState = choice1 }
                }
                addState(State2) { callbacks.listen(this) }
            }

            machine.processEventBlocking(SwitchEvent)

            verifySequence { callbacks.onEntryState(State2) }
        }

        "initial choice state currently not supported" {
            val callbacks = mockkCallbacks()

            shouldThrow<IllegalStateException> {
                createTestStateMachine(coroutineStarterType) {
                    val choice = choiceState("choice") { State2 }
                    setInitialState(choice)

                    addState(State2) { callbacks.listen(this) }
                }
            }
        }
    }
})