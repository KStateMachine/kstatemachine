package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.mockk.Called
import io.mockk.verify
import io.mockk.verifySequence

private open class HierarchyEventL1 : Event
private open class HierarchyEventL2 : HierarchyEventL1()

class EventMatcherTest : StringSpec({
    "EventMatcher::isEqual()" {
        val callbacks = mockkCallbacks()

        val machine = createStateMachine {
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

        machine.processEvent(event)

        verifySequence { callbacks.onTriggeredTransition(event) }
    }

    "EventMatcher::isEqual() negative" {
        val callbacks = mockkCallbacks()

        val machine = createStateMachine {
            initialState("state1") {
                transition<HierarchyEventL1> {
                    eventMatcher = isEqual()
                    callbacks.listen(this)
                }
            }
        }

        val event = HierarchyEventL2()

        machine.processEvent(event)

        verify { callbacks wasNot Called }
    }

    "EventMatcher::isInstanceOf()" {
        val callbacks = mockkCallbacks()

        val machine = createStateMachine {
            initialState("state1") {
                transition<HierarchyEventL1> {
                    eventMatcher = isInstanceOf()
                    callbacks.listen(this)
                }
            }
        }

        val event = HierarchyEventL2()

        machine.processEvent(event)

        verifySequence { callbacks.onTriggeredTransition(event) }
    }

    "EventMatcher::isInstanceOf() negative" {
        val callbacks = mockkCallbacks()

        val machine = createStateMachine {
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
            machine.processEvent(HierarchyEventL2())
        }
    }
})