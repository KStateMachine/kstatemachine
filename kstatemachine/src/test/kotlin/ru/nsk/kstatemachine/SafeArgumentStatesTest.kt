package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.isA
import com.nhaarman.mockitokotlin2.then
import io.kotest.assertions.throwables.shouldThrow
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException

private class NameEvent(override val arg: String) : ArgEvent<String>

class SafeArgumentStatesTest {
    @Test
    fun safeArgumentState() {
        lateinit var state2: ArgState<String>

        val machine = createStateMachine {
            state2 = argState("state2")

            initialState("state1") {
                argTransition<NameEvent, String> {
                    targetState = state2
                }
            }
        }

        shouldThrow<IllegalStateException> { state2.arg }

        val name = "testName"
        machine.processEvent(NameEvent(name))

        assertThat(state2.arg, equalTo(name))
    }
}