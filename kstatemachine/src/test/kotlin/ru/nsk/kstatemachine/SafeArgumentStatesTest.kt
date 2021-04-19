package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

private class NameEvent(override val data: String) : DataEvent<String>

class SafeArgumentStatesTest {
    @Test
    fun safeArgumentState() {
        lateinit var state2: DataState<String>
        lateinit var state4: UnitState

        val machine = createStateMachine {
            state2 = dataState("state2")

            initialState("state1") {
                dataTransition<NameEvent, String> {
                    targetState = state2
                }
                //FIXME forbid?
//                transition<SwitchEvent> {
//                    targetState = state2
//                }
                transition<NameEvent> {
                    targetState = state4
                }
            }
        }

        shouldThrow<IllegalStateException> { state2.data }

        val name = "testName"
        machine.processEvent(NameEvent(name))

        assertThat(state2.data, equalTo(name))
    }
}