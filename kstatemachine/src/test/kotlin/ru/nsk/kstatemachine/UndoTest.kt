package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class UndoTest : StringSpec({
    "undo not enabled" {
        val machine = createStateMachine {
            initialState()
        }

        shouldThrow<IllegalStateException> { machine.undo() }
    }

    "undo throws with throwing PendingEventHandler" {
        lateinit var state1: State
        lateinit var state2: State
        val machine = createStateMachine(enableUndo = true) {
            pendingEventHandler = throwingPendingEventHandler()
            state1 = initialState("state1") {
                transitionOn<SwitchEvent> { targetState = { state2 } }
            }
            state2 = state("state2") {
                onEntry { shouldThrow<IllegalStateException> { machine.undo() } }
            }
        }

        machine.activeStates() shouldContain state1
        machine.processEvent(SwitchEvent)
        machine.activeStates() shouldContain state2
    }

    "undo to initial state" {
        lateinit var state1: State
        lateinit var state2: State
        val machine = createStateMachine(enableUndo = true) {
            state1 = initialState("state1") {
                transitionOn<SwitchEvent> { targetState = { state2 } }
            }
            state2 = state("state2")
        }

        machine.activeStates() shouldContain state1
        machine.processEvent(SwitchEvent)
        machine.activeStates() shouldContain state2
        machine.undo()
        machine.activeStates() shouldContain state1
    }

    "undo multiple times" {
        lateinit var state1: State
        lateinit var state2: State
        lateinit var state3: State
        val machine = createStateMachine(enableUndo = true) {
            state1 = initialState("state1") {
                transitionOn<SwitchEvent> { targetState = { state2 } }
            }
            state2 = state("state2") {
                transitionOn<SwitchEvent> { targetState = { state3 } }
            }
            state3 = state("state3")
        }

        machine.activeStates() shouldContain state1
        machine.processEvent(SwitchEvent)
        machine.activeStates() shouldContain state2
        machine.processEvent(SwitchEvent)
        machine.activeStates() shouldContain state3
        machine.undo()
        machine.activeStates() shouldContain state2
        machine.undo()
        machine.activeStates() shouldContain state1
    }

    "undo cross-level transition" {
        lateinit var state12: State
        lateinit var state2: State
        val machine = createStateMachine(enableUndo = true) {
            initialState("state1") {
                initialState("state11") {
                    transitionOn<SwitchEvent> { targetState = { state12 } }
                }
                state12 = state("state12") {
                    transitionOn<SwitchEvent> { targetState = { state2 } }
                }
            }
            state2 = state("state2")
        }
        machine.processEvent(SwitchEvent)
        machine.activeStates().shouldContain(state12)
        machine.processEvent(SwitchEvent)
        machine.activeStates().shouldContain(state2)
        machine.processEvent(UndoEvent) // alternative syntax
        machine.activeStates().shouldContain(state12)
    }

    "undo with DataState" {
        lateinit var state12: DataState<Int>
        lateinit var state2: State
        val machine = createStateMachine(enableUndo = true) {
            initialState("state1") {
                initialState("state11") {
                    dataTransitionOn<SwitchDataEvent, Int> { targetState = { state12 } }
                }
                state12 = dataState("state12") {
                    transitionOn<SwitchEvent> { targetState = { state2 } }
                }
            }
            state2 = state("state2")
        }

        machine.processEvent(SwitchDataEvent(42))
        state12.data shouldBe 42
        machine.processEvent(SwitchEvent)
        machine.activeStates().shouldContain(state2)
        shouldThrow<IllegalStateException> { state12.data }
        machine.undo()
        machine.activeStates().shouldContain(state12)
        state12.data shouldBe 42
    }
}) {
    private class SwitchDataEvent(override val data: Int) : DataEvent<Int>
}