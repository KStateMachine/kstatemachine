package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.containExactlyInAnyOrder
import io.kotest.matchers.should
import io.mockk.verifySequence
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
        val callbacks = mockkCallbacks()

        lateinit var state1: State
        lateinit var state2: State

        val machine = createStateMachine(childMode = ChildMode.PARALLEL) {
            callbacks.listen(this)
            state1 = state { callbacks.listen(this) }
            state2 = state { callbacks.listen(this) }
        }

        verifySequence {
            callbacks.onEntryState(machine)
            callbacks.onEntryState(state1)
            callbacks.onEntryState(state2)
        }

        machine.activeStates() should containExactlyInAnyOrder(machine, state1, state2)

        machine.isActive.shouldBeTrue()
        state1.isActive.shouldBeTrue()
        state2.isActive.shouldBeTrue()
    }

    @Test
    fun enterParallelStates() {
        val callbacks = mockkCallbacks()

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

        verifySequence {
            callbacks.onEntryState(state1)
            callbacks.onEntryState(state11)
            callbacks.onEntryState(state111)
            callbacks.onEntryState(state12)
        }

        machine.activeStates() should containExactlyInAnyOrder(machine, state1, state11, state12, state111)
    }

    @Test
    fun exitParallelStates() {
        val callbacks = mockkCallbacks()

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

        verifySequenceAndClear(callbacks) {
            callbacks.onEntryState(state1)
            callbacks.onEntryState(state11)
            callbacks.onEntryState(state12)
        }

        machine.processEvent(SwitchEvent)

        verifySequence {
            callbacks.onExitState(state11)
            callbacks.onExitState(state12)
            callbacks.onExitState(state1)
            callbacks.onEntryState(state2)
        }
    }

    @Test
    fun processEventByParallelStates() {
        val callbacks = mockkCallbacks()

        val machine = createStateMachine {
            initialState(childMode = ChildMode.PARALLEL) {
                state {
                    transition<SwitchEvent> {
                        onTriggered { callbacks.onTriggeredTransition(it.event, 1) }
                    }
                }
                state {
                    transition<SwitchEvent> {
                        onTriggered { callbacks.onTriggeredTransition(it.event, 2) }
                    }
                }
            }
        }

        machine.processEvent(SwitchEvent)

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEvent, 1)
            callbacks.onTriggeredTransition(SwitchEvent, 2)
        }
    }
}