package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.verifySequence
import ru.nsk.kstatemachine.ProcessingResult.PENDING
import ru.nsk.kstatemachine.ProcessingResult.PROCESSED

class PendingEventHandlerTest : StringSpec({
    CoroutineStarterType.values().forEach { coroutineStarterType ->
        "queue event in QueuePendingEventHandler" {
            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it) }

                val third = state("third")
                val second = state("second") {
                    transition<SecondEvent>(targetState = third)
                }
                initialState("first") {
                    transition<FirstEvent> {
                        targetState = second
                        onTriggered { this@createTestStateMachine.processEventCo(SecondEvent) shouldBe PENDING }
                    }
                }
            }

            machine.processEvent(FirstEvent) shouldBe PROCESSED
        }

        "queue event on machine start" {
            val callbacks = mockkCallbacks()
            createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it) }

                val second = state("second")

                initialState("first") {
                    onEntry { machine.processEventCo(SwitchEvent) }
                    initialState("first internal")

                    transition<SwitchEvent> {
                        targetState = second
                        callbacks.listen(this)
                    }
                }
            }

            verifySequence { callbacks.onTriggeredTransition(SwitchEvent) }
        }

        "pending event queue is cleared on processing error" {
            val machine = createTestStateMachine(coroutineStarterType, start = false) {
                logger = StateMachine.Logger { println(it) }

                val second = state("second") {
                    transition<SecondEvent>()
                }

                initialState("first") {
                    onEntry {
                        machine.processEventCo(FirstEvent)
                        machine.processEventCo(FirstEvent)
                        machine.processEventCo(FirstEvent)
                    }
                    initialState("first internal")

                    transition<FirstEvent>(targetState = second)
                }
                ignoredEventHandler = StateMachine.IgnoredEventHandler {
                    throw TestException("test")
                }
            }

            shouldThrow<TestException> { machine.start() }

            machine.processEvent(SecondEvent)
        }

        "throwing PendingEventHandler does not destroy machine" {
            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it) }

                val second = state("second")
                initialState("first") {
                    transition<SwitchEvent> {
                        targetState = second
                        onTriggered {
                            shouldThrow<TestException> { this@createTestStateMachine.processEventCo(SwitchEvent) }
                        }
                    }
                }

                pendingEventHandler = StateMachine.PendingEventHandler {
                    testError("Already processing")
                }
            }

            machine.processEvent(SwitchEvent)
            machine.isDestroyed shouldBe false
        }

        "pending events are cleared on stop() from notification callback" {
            val machine = createTestStateMachine(coroutineStarterType) {
                val state2 = state("state2") {
                    onEntry {
                        machine.processEventCo(SwitchEvent) shouldBe PENDING
                        machine.processEventCo(SwitchEvent) shouldBe PENDING
                        machine.stop()
                    }
                }
                initialState("state1") {
                    transition<SwitchEvent>(targetState = state2)
                }
            }
            machine.processEvent(SwitchEvent)
            machine.isRunning shouldBe false
            machine.start()
        }

        "pending events are cleared on destroy() from notification callback" {
            val machine = createTestStateMachine(coroutineStarterType) {
                val state2 = state("state2") {
                    onEntry {
                        machine.processEventCo(SwitchEvent) shouldBe PENDING
                        machine.processEventCo(SwitchEvent) shouldBe PENDING
                        machine.destroy(false)
                    }
                }
                initialState("state1") {
                    transition<SwitchEvent>(targetState = state2)
                }
            }
            machine.processEvent(SwitchEvent)
            machine.isDestroyed shouldBe true
        }
    }
})