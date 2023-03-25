package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.mockk.called
import io.mockk.verify
import io.mockk.verifySequence
import ru.nsk.kstatemachine.EventMatcherTestData.HierarchyEventL1
import ru.nsk.kstatemachine.EventMatcherTestData.HierarchyEventL2

private object EventMatcherTestData {
    open class HierarchyEventL1 : Event
    open class HierarchyEventL2 : HierarchyEventL1()
}

class EventMatcherTest : StringSpec({
    CoroutineStarterType.values().forEach { coroutineStarterType ->
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

            shouldThrow<IllegalStateException> {
                machine.processEventBlocking(HierarchyEventL2())
            }
        }
    }
})