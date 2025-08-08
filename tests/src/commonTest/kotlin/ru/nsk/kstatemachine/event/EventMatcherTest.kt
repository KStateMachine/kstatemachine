/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.event

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldStartWith
import io.mockk.called
import io.mockk.verify
import io.mockk.verifySequence
import ru.nsk.kstatemachine.CoroutineStarterType
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.event.EventMatcherTestData.HierarchyEventL1
import ru.nsk.kstatemachine.event.EventMatcherTestData.HierarchyEventL2
import ru.nsk.kstatemachine.listen
import ru.nsk.kstatemachine.mockkCallbacks
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.statemachine.processEventBlocking

private object EventMatcherTestData {
    open class HierarchyEventL1 : Event
    open class HierarchyEventL2 : HierarchyEventL1()
}

class EventMatcherTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "isEqual()" {
                val callbacks = mockkCallbacks()

                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState("state1") {
                        transition<HierarchyEventL1> {
                            eventMatcher = isEqual()
                            callbacks.listen(this)
                        }
                        transition<HierarchyEventL2> {
                            eventMatcher = isEqual()
                            callbacks.listen(this)
                        }
                    }
                }

                val event = HierarchyEventL2()

                machine.processEventBlocking(event)

                verifySequence { callbacks.onTransitionTriggered(event) }
            }

            "isEqual() negative" {
                val callbacks = mockkCallbacks()

                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState("state1") {
                        transition<HierarchyEventL1> {
                            eventMatcher = isEqual()
                            callbacks.listen(this)
                        }
                    }
                }

                val event = HierarchyEventL2()

                machine.processEventBlocking(event)

                verify { callbacks wasNot called }
            }

            "isInstanceOf()" {
                val callbacks = mockkCallbacks()

                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState("state1") {
                        transition<HierarchyEventL1> {
                            eventMatcher = isInstanceOf()
                            callbacks.listen(this)
                        }
                    }
                }

                val event = HierarchyEventL2()

                machine.processEventBlocking(event)

                verifySequence { callbacks.onTransitionTriggered(event) }
            }

            "isInstanceOf() negative" {
                val callbacks = mockkCallbacks()

                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState("state1") {
                        transition<HierarchyEventL1> {
                            eventMatcher = isInstanceOf()
                            callbacks.listen(this)
                        }
                        transition<HierarchyEventL2> {
                            eventMatcher = isInstanceOf()
                            callbacks.listen(this)
                        }
                    }
                }

                val exception = shouldThrow<IllegalStateException> {
                    machine.processEventBlocking(HierarchyEventL2())
                }
                exception.message shouldStartWith "Multiple transitions match"
            }
        }
    }
})