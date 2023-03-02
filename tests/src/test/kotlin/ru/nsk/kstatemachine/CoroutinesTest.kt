package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.*
import java.lang.UnsupportedOperationException
import kotlin.coroutines.EmptyCoroutineContext

class CoroutinesTest : StringSpec({
    /** Coroutines manipulations like withContext or launch from coroutineScope make test fail. */
    "call suspend functions from major listeners and callbacks" {
        val machine = createStateMachine {
            onStarted {
                delay(0)
            }
            onStopped { delay(0) }
            onTransition { delay(0) }
            onTransitionComplete { _, _ -> delay(0) }
            onStateEntry { delay(0) }
            val first = initialState("first") {
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
            choiceState {
                delay(0)
                first
            }
        }
        machine.processEvent(SwitchEvent)
    }

    "using coroutines with std lib throws" {
        shouldThrow<UnsupportedOperationException> {
            createStateMachine {
                initialState()
                onStarted { delay(1) }
            }
        }
    }

    "processEventAsync should throw with std lib implementation" {
        val machine = createStateMachine {
            initialState()
        }
        shouldThrow<UnsupportedOperationException> {
            machine.processEventAsync(SwitchEvent)
        }
    }

    "test coroutines called from machine callbacks" {
        val scope = CoroutineScope(EmptyCoroutineContext)
        try {
            createCoStateMachine(scope) {
                onStarted { delay(1) }
                initialState("first") {
                    onEntry {
                        coroutineScope {
                            launch { delay(1) }
                            launch { delay(1) }
                        }
                        withContext(Dispatchers.Default) {
                            delay(1)
                        }
                    }
                }
            }
        } finally {
            scope.cancel()
        }
    }
})