package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException

// FIXME add cross level transition test
class NestedStateTest {
    @Test
    fun enterNestedState() {
        val callbacks = mock<Callbacks>()
        val inOrder = inOrder(callbacks)

        lateinit var firstL1: State
        lateinit var firstL2: State
        val firstL3 = object : DefaultState("firstL3") {}

        createStateMachine {
            firstL1 = initialState("firstL1") {
                onEntry { callbacks.onEntryState(this) }

                firstL2 = initialState("firstL2") {
                    onEntry { callbacks.onEntryState(this) }

                    addInitialState(firstL3) {
                        onEntry { callbacks.onEntryState(this) }
                    }
                }
            }
        }

        then(callbacks).should(inOrder).onEntryState(firstL1)
        then(callbacks).should(inOrder).onEntryState(firstL2)
        then(callbacks).should(inOrder).onEntryState(firstL3)
    }

    @Test
    fun nestedStateFinishL2() {
        val callbacks = mock<Callbacks>()
        val inOrder = inOrder(callbacks)

        lateinit var initialL1: State
        lateinit var finalL1: State
        lateinit var initialL2: State
        lateinit var finalL2: State

        val machine = createStateMachine {
            logger = StateMachine.Logger { println(it) }

            finalL1 = finalState("finalL1") {
                onEntry { callbacks.onEntryState(this) }
                onExit { callbacks.onExitState(this) }
            }

            initialL1 = initialState("initialL1") {
                onEntry { callbacks.onEntryState(this) }
                onExit { callbacks.onExitState(this) }
                onFinished { callbacks.onFinished(this) }

                transition<SwitchEventL1> {
                    targetState = finalL1
                    onTriggered { callbacks.onTriggeredTransition(it.event) }
                }

                finalL2 = finalState("finalL2") {
                    onEntry { callbacks.onEntryState(this) }
                    onExit { callbacks.onExitState(this) }
                }

                initialL2 = initialState("initialL2") {
                    onEntry { callbacks.onEntryState(this) }
                    onExit { callbacks.onExitState(this) }
                    transition<SwitchEventL2> {
                        targetState = finalL2
                        onTriggered { callbacks.onTriggeredTransition(it.event) }
                    }
                }
            }

            onFinished { callbacks.onFinished(this) }
        }

        then(callbacks).should(inOrder).onEntryState(initialL1)
        then(callbacks).should(inOrder).onEntryState(initialL2)
        then(callbacks).shouldHaveNoMoreInteractions()

        machine.processEvent(SwitchEventL2)

        then(callbacks).should(inOrder).onTriggeredTransition(SwitchEventL2)
        then(callbacks).should(inOrder).onExitState(initialL2)
        then(callbacks).should(inOrder).onEntryState(finalL2)
        then(callbacks).should(inOrder).onFinished(initialL1)
        then(callbacks).shouldHaveNoMoreInteractions()

        machine.processEvent(SwitchEventL1)

        then(callbacks).should(inOrder).onTriggeredTransition(SwitchEventL1)
        then(callbacks).should(inOrder).onExitState(initialL1)
        then(callbacks).should(inOrder).onEntryState(finalL1)
        then(callbacks).should(inOrder).onFinished(machine)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun nestedNoInitialState() {
        val machine = createStateMachine(start = false) {
            initialState("firstL1") {
                state("firstL2")
            }
        }

        shouldThrow<IllegalStateException> { machine.start() }
    }
}