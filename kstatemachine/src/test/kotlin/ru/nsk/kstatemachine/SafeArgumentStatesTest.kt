package ru.nsk.kstatemachine

import org.junit.jupiter.api.Test

private class StringArgumentEvent(override val argument: String) : ArgEvent<String>

class SafeArgumentStatesTest {
    @Test
    fun safeArgumentState() {
        val machine = createStateMachine {
            val state2 = argState<String>("state2")
            val state3 = argState<Int>("state3")

            initialState("state1") {
                // FIXME how I can remove String argument?
                argTransition<String, StringArgumentEvent> {
                    targetState = state2
                }
                // desired
                argTransition<StringArgumentEvent> {
                    targetState = state2
                }
                // this can only give runtime error
                transition<StringArgumentEvent> {
                    targetState = state3
                }
            }
        }
    }
}