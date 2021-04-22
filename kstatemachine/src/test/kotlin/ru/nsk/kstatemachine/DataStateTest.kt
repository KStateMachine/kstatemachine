package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import io.kotest.assertions.throwables.shouldThrow
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

private class NameEvent(override val data: String) : DataEvent<String>
private class IdEvent(override val data: Int) : DataEvent<Int>

class DataStateTest {
    @Test
    fun initialDataState_negative() {
        shouldThrow<Exception> {
            createStateMachine {
                addInitialState(DefaultState<String>("state1"))
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

        assertThat(state2.data, equalTo(name))
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

        assertThat(state2.data, equalTo(name))

        val id = 42
        machine.processEvent(IdEvent(id))

        shouldThrow<IllegalStateException> { state2.data }
        assertThat(state3.data, equalTo(id))
    }

    @Test
    fun multipleNestedDataStates() {
        val callbacks = mock<Callbacks>()
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

        then(callbacks).should().onEntryState(state1)

        val name = "testName"
        machine.processEvent(NameEvent(name))
        then(callbacks).should().onExitState(state1)
        then(callbacks).should().onEntryState(state2)
        then(callbacks).should().onEntryState(state21)

        assertThat(state2.data, equalTo(name))

        val id = 42
        machine.processEvent(IdEvent(id))
        then(callbacks).should().onExitState(state21)
        then(callbacks).should().onEntryState(state22)
        then(callbacks).shouldHaveNoMoreInteractions()

        assertThat(state2.data, equalTo(name))
        assertThat(state22.data, equalTo(id))
    }
}