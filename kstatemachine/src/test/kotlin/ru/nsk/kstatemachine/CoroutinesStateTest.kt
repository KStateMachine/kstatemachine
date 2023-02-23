package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.delay

class CoroutinesStateTest : StringSpec({
    "call suspend function in a state listener" {
        createStateMachine {
            initialState("first") {
                onEntry { delay(0) }
                onExit { delay(0) }

                transition<SwitchEvent> {
                    onTriggered { delay(0) }
                }
            }
        }
    }
})