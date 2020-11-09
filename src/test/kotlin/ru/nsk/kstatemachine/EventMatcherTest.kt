package ru.nsk.kstatemachine

import org.junit.jupiter.api.Test

class EventMatcherTest {
    @Test
    fun eventMatcher() {
        val stateMachine = createStateMachine {
            val first = state("first") {
                transition<SwitchEvent> {
                    eventMatcher = isEqual()
                    eventMatcher = isInstanceOf()
                }
            }
            setInitialState(first)
        }
    }
}