package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

private open class HierarchyEventL1 : Event
private open class HierarchyEventL2 : HierarchyEventL1()

class EventMatcherTest {
    @Test
    fun eventMatcherIsEqual() {
        val callbacks = mock<Callbacks>()

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

        then(callbacks).should().onTriggeredTransition(event)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun eventMatcherIsEqualNegative() {
        val callbacks = mock<Callbacks>()

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

        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun eventMatcherInstanceOf() {
        val callbacks = mock<Callbacks>()

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

        then(callbacks).should().onTriggeredTransition(event)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun eventMatcherInstanceOfNegative() {
        val callbacks = mock<Callbacks>()

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
}