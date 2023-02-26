package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.*

class StdLibCoroutinesTest : StringSpec({
    /** Coroutines manipulations like withContext or launch from coroutineScope make test fail. */
    "call suspend functions from major listeners and callbacks" {
        val machine = createStateMachine {
            onStarted {
                delay(0)
                Thread.sleep(10)
            }
            onStopped { delay(0) }
            onTransition { delay(0) }
            onTransitionComplete { _ ,_ -> delay(0) }
            onStateEntry { delay(0) }
            initialState("first") {
                onEntry { delay(0) }
                onExit { delay(0) }
                onFinished { delay(0) }

                val transition = transition<SwitchEvent> {
                    guard = {
                        delay(0)
                        true
                    }
                    onTriggered { delay(0) }
                }
                transition.onTriggered { delay(0) }

                transitionConditionally<SecondEvent> {
                    direction = {
                        delay(0)
                        stay()
                    }
                }
            }
        }
        machine.processEvent(SwitchEvent)
    }
})