package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.verifySequence

class PendingEventHandlerTest : StringSpec({
    "queue event in QueuePendingEventHandler" {
        val machine = createStateMachine {
            logger = StateMachine.Logger { println(it) }

            val third = state("third")
            val second = state("second") {
                transition<SecondEvent>(targetState = third)
            }
            initialState("first") {
                transition<FirstEvent> {
                    targetState = second
                    onTriggered { this@createStateMachine.processEvent(SecondEvent) }
                }
            }
        }

        machine.processEvent(FirstEvent)
    }

    "queue event on machine start" {
        val callbacks = mockkCallbacks()
        createStateMachine {
            logger = StateMachine.Logger { println(it) }

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

        verifySequence { callbacks.onTriggeredTransition(SwitchEvent) }
    }

    "pending event queue is cleared on processing error" {
        val machine = createStateMachine(start = false) {
            logger = StateMachine.Logger { println(it) }

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
            ignoredEventHandler = StateMachine.IgnoredEventHandler { _, _ ->
                throw TestException("test")
            }
        }

        shouldThrow<TestException> { machine.start() }

        machine.processEvent(SecondEvent)
    }

    "throwing PendingEventHandler does not destroy machine" {
        val machine = createStateMachine {
            logger = StateMachine.Logger { println(it) }

            val second = state("second")
            initialState("first") {
                transition<SwitchEvent> {
                    targetState = second
                    onTriggered {
                        shouldThrow<TestException> { this@createStateMachine.processEvent(SwitchEvent) }
                    }
                }
            }

            pendingEventHandler = StateMachine.PendingEventHandler { _, _ ->
                testError("Already processing")
            }
        }

        machine.processEvent(SwitchEvent)
        machine.isDestroyed shouldBe false
    }
})