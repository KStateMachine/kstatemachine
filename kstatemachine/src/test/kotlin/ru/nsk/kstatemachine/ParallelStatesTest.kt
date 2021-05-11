package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.containExactlyInAnyOrder
import io.kotest.matchers.should
import org.junit.jupiter.api.Test

class ParallelStatesTest {
    @Test
    fun initialStateInParallelMode_negative() {
        createStateMachine {
            initialState(childMode = ChildMode.PARALLEL) {
                shouldThrow<IllegalStateException> { initialState() }
            }
        }
    }

    @Test
    fun parallelStateMachine() {
        val callbacks = mock<Callbacks>()
        val inOrder = inOrder(callbacks)

        lateinit var state1: State
        lateinit var state2: State

        val machine = createStateMachine(childMode = ChildMode.PARALLEL) {
            callbacks.listen(this)
            state1 = state { callbacks.listen(this) }
            state2 = state { callbacks.listen(this) }
        }

        then(callbacks).should(inOrder).onEntryState(machine)
        then(callbacks).should(inOrder).onEntryState(state1)
        then(callbacks).should(inOrder).onEntryState(state2)
        then(callbacks).shouldHaveNoMoreInteractions()

        machine.activeStates() should containExactlyInAnyOrder(machine, state1, state2)

        assert(machine.isActive)
        assert(state1.isActive)
        assert(state2.isActive)
    }

    @Test
    fun enterParallelStates() {
        val callbacks = mock<Callbacks>()
        val inOrder = inOrder(callbacks)

        lateinit var state1: State
        lateinit var state11: State
        lateinit var state12: State
        lateinit var state111: State
        val machine = createStateMachine {
            state1 = initialState(childMode = ChildMode.PARALLEL) {
                callbacks.listen(this)

                state11 = state {
                    callbacks.listen(this)

                    state111 = initialState { callbacks.listen(this) }
                }
                state12 = state { callbacks.listen(this) }
            }
        }

        then(callbacks).should(inOrder).onEntryState(state1)
        then(callbacks).should(inOrder).onEntryState(state11)
        then(callbacks).should(inOrder).onEntryState(state111)
        then(callbacks).should(inOrder).onEntryState(state12)
        then(callbacks).shouldHaveNoMoreInteractions()

        machine.activeStates() should containExactlyInAnyOrder(machine, state1, state11, state12, state111)
    }

    @Test
    fun exitParallelStates() {
        val callbacks = mock<Callbacks>()
        val inOrder = inOrder(callbacks)

        lateinit var state1: State
        lateinit var state2: State
        lateinit var state11: State
        lateinit var state12: State

        val machine = createStateMachine {
            state1 = initialState("state1", childMode = ChildMode.PARALLEL) {
                callbacks.listen(this)

                state11 = state("state11") { callbacks.listen(this) }
                state12 = state("state12") { callbacks.listen(this) }

                transitionOn<SwitchEvent> { targetState = { state2 } }
            }
            state2 = state("state2") { callbacks.listen(this) }
        }

        then(callbacks).should(inOrder).onEntryState(state1)
        then(callbacks).should(inOrder).onEntryState(state11)
        then(callbacks).should(inOrder).onEntryState(state12)

        machine.processEvent(SwitchEvent)

        then(callbacks).should(inOrder).onExitState(state11)
        then(callbacks).should(inOrder).onExitState(state12)
        then(callbacks).should(inOrder).onExitState(state1)
        then(callbacks).should(inOrder).onEntryState(state2)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun processEventByParallelStates() {
        val callbacks = mock<Callbacks>()

        val machine = createStateMachine {
            initialState(childMode = ChildMode.PARALLEL) {
                state {
                    transition<SwitchEvent> { callbacks.listen(this) }
                }
                state {
                    transition<SwitchEvent> { callbacks.listen(this) }
                }
            }
        }

        machine.processEvent(SwitchEvent)

        then(callbacks).should().onTriggeredTransition(SwitchEvent)
        then(callbacks).should().onTriggeredTransition(SwitchEvent)
        then(callbacks).shouldHaveNoMoreInteractions()
    }
}