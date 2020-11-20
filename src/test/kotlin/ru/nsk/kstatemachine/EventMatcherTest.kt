package ru.nsk.kstatemachine

import org.junit.jupiter.api.Test

class EventMatcherTest {
    @Test
    fun eventMatcher() {
        createStateMachine {
            initialState("first") {
                transition<SwitchEvent> {
                    eventMatcher = isEqual()
                    eventMatcher = isInstanceOf()
                }
            }
        }
    }
}