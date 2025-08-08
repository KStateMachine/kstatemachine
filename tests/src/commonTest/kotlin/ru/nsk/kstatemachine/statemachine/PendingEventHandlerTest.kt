/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.statemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.verifySequence
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.onEntry
import ru.nsk.kstatemachine.state.state
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.statemachine.ProcessingResult.PENDING
import ru.nsk.kstatemachine.statemachine.ProcessingResult.PROCESSED
import ru.nsk.kstatemachine.transition.onTriggered

class PendingEventHandlerTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "queue event in QueuePendingEventHandler" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    logger = StateMachine.Logger { println(it()) }

                    val third = state("third")
                    val second = state("second") {
                        transition<SecondEvent>(targetState = third)
                    }
                    initialState("first") {
                        transition<FirstEvent> {
                            targetState = second
                            onTriggered { this@createTestStateMachine.processEvent(SecondEvent) shouldBe PENDING }
                        }
                    }
                }

                machine.processEventBlocking(FirstEvent) shouldBe PROCESSED
            }

            "queue event on machine start" {
                val callbacks = mockkCallbacks()
                createTestStateMachine(coroutineStarterType) {
                    logger = StateMachine.Logger { println(it()) }

                    val second = state("second")

                    initialState("first") {
                        onEntry { machine.processEvent(SwitchEvent) }
                        initialState("first internal")

                        transition<SwitchEvent> {
                            targetState = second
                            callbacks.listen(this)
                        }
                    }
                }

                verifySequence { callbacks.onTransitionTriggered(SwitchEvent) }
            }

            "pending event queue is cleared on processing error" {
                val machine = createTestStateMachine(coroutineStarterType, start = false) {
                    logger = StateMachine.Logger { println(it()) }

                    val second = state("second") {
                        transition<SecondEvent>()
                    }

                    initialState("first") {
                        onEntry {
                            machine.processEvent(FirstEvent)
                            machine.processEvent(FirstEvent)
                            machine.processEvent(FirstEvent)
                        }
                        initialState("first internal")

                        transition<FirstEvent>(targetState = second)
                    }
                    ignoredEventHandler = StateMachine.IgnoredEventHandler {
                        throw TestException("test")
                    }
                }

                shouldThrow<TestException> { machine.startBlocking() }

                machine.processEventBlocking(SecondEvent)
            }

            "throwing PendingEventHandler does not destroy machine" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    logger = StateMachine.Logger { println(it()) }

                    val second = state("second")
                    initialState("first") {
                        transition<SwitchEvent> {
                            targetState = second
                            onTriggered {
                                shouldThrow<TestException> { this@createTestStateMachine.processEvent(SwitchEvent) }
                            }
                        }
                    }

                    pendingEventHandler = StateMachine.PendingEventHandler {
                        testError("Already processing")
                    }
                }

                machine.processEventBlocking(SwitchEvent)
                machine.isDestroyed shouldBe false
            }

            "pending events are cleared on stop() from notification callback" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    val state2 = state("state2") {
                        onEntry {
                            machine.processEvent(SwitchEvent) shouldBe PENDING
                            machine.processEvent(SwitchEvent) shouldBe PENDING
                            machine.stop()
                        }
                    }
                    initialState("state1") {
                        transition<SwitchEvent>(targetState = state2)
                    }
                }
                machine.processEventBlocking(SwitchEvent)
                machine.isRunning shouldBe false
                machine.startBlocking()
            }

            "pending events are cleared on destroy() from notification callback" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    val state2 = state("state2") {
                        onEntry {
                            machine.processEvent(SwitchEvent) shouldBe PENDING
                            machine.processEvent(SwitchEvent) shouldBe PENDING
                            machine.destroy(false)
                        }
                    }
                    initialState("state1") {
                        transition<SwitchEvent>(targetState = state2)
                    }
                }
                machine.processEventBlocking(SwitchEvent)
                machine.isDestroyed shouldBe true
            }
        }
    }
})