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
                onTransitionTriggered { log { it.toString() } }
            }

            machine.processEventBlocking(SwitchEvent, false)

            verifySequence { callbacks.onStateEntry(State2) }
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

            verifySequence { callbacks.onStateEntry(State2) }
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

        "redirecting choice data state" {
            val callbacks = mockkCallbacks()

            class IntEvent(override val data: Int) : DataEvent<Int>

            lateinit var intState1: DataState<Int>
            lateinit var intState2: DataState<Int>

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it()) }

                addInitialState(State1)

                val choice = choiceDataState("data choice") {
                    log { "$event $argument" }
                    val intEvent = event as? IntEvent // cast is necessary as we don't know event type here
                    if (intEvent?.data == 42) intState1 else intState2
                }

                dataTransition<IntEvent, Int> { targetState = choice }

                intState1 = dataState<Int>("intState1") { callbacks.listen(this) }
                intState2 = dataState<Int>("intState2") { callbacks.listen(this) }
                onTransitionTriggered { log { it.toString() } }
            }

            machine.processEvent(IntEvent(42), true)
            verifySequenceAndClear(callbacks) { callbacks.onStateEntry(intState1) }
            machine.processEvent(IntEvent(66), false)
            verifySequenceAndClear(callbacks) {
                callbacks.onStateExit(intState1)
                callbacks.onStateEntry(intState2)
            }
        }
    }
})