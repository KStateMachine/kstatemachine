package ru.nsk.kstatemachine

import org.junit.jupiter.api.Test

private class NameEvent(override val arg: String) : ArgEvent<String>

class SafeArgumentStatesTest {
    @Test
    fun safeArgumentState() {
        val machine = createStateMachine {
            val state2 = argState<String>("state2")
            val state3 = argState<Int>("state3")

            initialState("state1") {
                // FIXME how I can remove String argument?
                argTransition<NameEvent, String> {
                    targetState = state2
                }

                // this can only give runtime error
                transition<NameEvent> {
                    targetState = state3
                }
            }
        }
    }
}