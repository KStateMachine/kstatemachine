package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.verifySequence
import org.junit.jupiter.api.Test

private class NameEvent(override val data: String) : DataEvent<String>
private class IdEvent(override val data: Int) : DataEvent<Int>

class TypesafeTransitionTest {
    @Test
    fun initialDataState_negative() {
        shouldThrow<Exception> {
            createStateMachine {
                addInitialState(DefaultDataState<String>("state1"))
            }
        }
    }

    @Test
    fun singleDataState() {
        lateinit var state2: DataState<String>

        val machine = createStateMachine {
            state2 = dataState("state2")

            initialState("state1") {
                dataTransition<NameEvent, String> { targetState = state2 }
            }
        }

        shouldThrow<IllegalStateException> { state2.data }

        val name = "testName"
        machine.processEvent(NameEvent(name))

        state2.data shouldBe name
    }

    @Test
    fun multipleDataStates() {
        lateinit var state2: DataState<String>
        lateinit var state3: DataState<Int>

        val machine = createStateMachine {
            initialState("state1") {
                dataTransitionOn<NameEvent, String> { targetState = { state2 } }
            }
            state2 = dataState("state2") {
                dataTransitionOn<IdEvent, Int> { targetState = { state3 } }
            }
            state3 = dataState("state3")
        }

        val name = "testName"
        machine.processEvent(NameEvent(name))

        state2.data shouldBe name

        val id = 42
        machine.processEvent(IdEvent(id))

        shouldThrow<IllegalStateException> { state2.data }
        state3.data shouldBe id
    }

    @Test
    fun multipleNestedDataStates() {
        val callbacks = mockkCallbacks()
        lateinit var state1: State
        lateinit var state2: DataState<String>
        lateinit var state21: State
        lateinit var state22: DataState<Int>

        val machine = createStateMachine {
            state1 = initialState("state1") {
                callbacks.listen(this)

                dataTransitionOn<NameEvent, String> { targetState = { state2 } }
            }
            state2 = dataState("state2") {
                callbacks.listen(this)

                state21 = initialState("state21") {
                    callbacks.listen(this)

                    dataTransitionOn<IdEvent, Int> { targetState = { state22 } }
                }
                state22 = dataState("state22") { callbacks.listen(this) }
            }
        }

        verifySequenceAndClear(callbacks) { callbacks.onEntryState(state1) }

        val name = "testName"
        machine.processEvent(NameEvent(name))
        verifySequenceAndClear(callbacks) {
            callbacks.onExitState(state1)
            callbacks.onEntryState(state2)
            callbacks.onEntryState(state21)
        }

        state2.data shouldBe name

        val id = 42
        machine.processEvent(IdEvent(id))
        verifySequence {
            callbacks.onExitState(state21)
            callbacks.onEntryState(state22)
        }

        state2.data shouldBe name
        state22.data shouldBe id
    }

    @Test
    fun implicitDataStateActivationByCrossLevelTransition_negative() {
        val machine = createStateMachine {
            lateinit var state21: State

            initialState {
                transitionOn<SwitchEvent> { targetState = { state21 } }
            }
            dataState<Int> {
                onEntry { println(data) }

                state21 = initialState()
            }
        }

        shouldThrow<IllegalStateException> { machine.processEvent(SwitchEvent) }
    }

    @Test
    fun transitionWithEventSuperType() {
        lateinit var state2: DataState<Number>

        val machine = createStateMachine {
            state2 = dataState("state2")

            initialState("state1") {
                dataTransition<IdEvent, Number> { targetState = state2 }
            }
        }

        val id = 42
        machine.processEvent(IdEvent(id))

        state2.data shouldBe id
    }
}