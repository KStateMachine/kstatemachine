package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.verify
import io.mockk.verifyOrder
import io.mockk.verifySequence

/**
 * In a parent state machine it is not possible to use as transitions targets states from inner machine and vise versa.
 * Inner machine is treated as atomic state by outer one.
 * Inner machine is started automatically when outer one enters it.
 */
class CompositionStateMachinesTest : StringSpec({
    "composition, inner machine auto start on entry" { composition(false) }
    "composition, inner machine already started" { composition(true) }

    "nested machine as initial state" {
        val callbacks = mockkCallbacks()
        lateinit var state1: IState
        val inner = createStateMachine(start = false) {
            state1 = initialState("state1") {
                callbacks.listen(this)
            }
        }

        createStateMachine {
            addInitialState(inner)
        }

        verifySequence {
            callbacks.onEntryState(state1)
        }
    }

    "stop nested machines" {
        val callbacks = mockkCallbacks()
        val inner = createStateMachine {
            initialState("state1") {
                callbacks.listen(this)
            }
        }

        val outer = createStateMachine {
            addInitialState(inner)
        }

        outer.isActive shouldBe true
        inner.isActive shouldBe true
        outer.stop()
        outer.isActive shouldBe false
        inner.isActive shouldBe true
    }

    "exit branch with nested machine" {
        val callbacks = mockkCallbacks()

        val inner = createStateMachine {
            initialState { callbacks.listen(this) }
        }

        lateinit var state1: State
        lateinit var state2: State
        val outer = createStateMachine {
            state1 = initialState {
                addInitialState(inner)
                transitionOn<SwitchEvent> { targetState = { state2 } }
            }
            state2 = state("state2")
        }

        outer.activeStates().shouldContainExactly(state1, inner)

        outer.processEvent(SwitchEvent)
        outer.isActive shouldBe true
        inner.isActive shouldBe true
        outer.activeStates().shouldContainExactly(state2)
    }

    "transition out from nested machine, negative" {
        lateinit var state1: State
        val inner = createStateMachine("inner") {
            logger = StateMachine.Logger { println(it) }
            state1 = initialState("inner-state1")
        }

        lateinit var state2: State
        val outer = createStateMachine("outer") {
            logger = StateMachine.Logger { println(it) }

            addInitialState(inner) {
                transitionOn<SwitchEvent> { targetState = { state2 } } // invalid
            }
            state2 = state("outer-state2")
        }

        inner.activeStates().shouldContainExactly(state1)

        outer.processEvent(SwitchEvent) // ignored
        inner.activeStates().shouldContainExactly(state1)
        outer.activeStates().shouldContainExactly(inner)
    }

    "transition into nested machine sub-state, negative" {
        lateinit var innerState2: State
        val inner = createStateMachine("inner") {
            logger = StateMachine.Logger { println(it) }
            initialState("inner-state1")
            innerState2 = state("inner-state2")
        }

        val outer = createStateMachine("outer") {
            logger = StateMachine.Logger { println(it) }

            initialState("state1") {
                transitionOn<SwitchEvent> { targetState = { innerState2 } } // invalid
            }
            addState(inner)
        }

        shouldThrow<IllegalStateException> { outer.processEvent(SwitchEvent) }
    }
})

private fun composition(startInnerMachineOnSetup: Boolean) {
    val callbacks = mockkCallbacks()

    val outerState1 = DefaultState("Outer state1")
    val innerState1 = DefaultState("Inner state1")
    val innerState2 = DefaultState("Inner state2")

    val innerMachine = createStateMachine("Inner machine", start = startInnerMachineOnSetup) {
        logger = StateMachine.Logger { println(it) }

        callbacks.listen(this)

        onStarted {
            callbacks.onStarted(this)
        }

        addInitialState(innerState1) {
            callbacks.listen(this)

            transition<SwitchEvent>("Switch") {
                targetState = innerState2

                callbacks.listen(this)
            }
        }

        addState(innerState2) {
            callbacks.listen(this)
        }
    }

    val machine = createStateMachine {
        callbacks.listen(this)

        addInitialState(outerState1) {
            callbacks.listen(this)

            transition<SwitchEvent> {
                targetState = innerMachine

                callbacks.listen(this)
            }
        }

        addState(innerMachine)
    }

    verifyOrder {
        callbacks.onEntryState(machine)
        callbacks.onEntryState(outerState1)
    }
    clearMocks(callbacks, answers = false)

    machine.processEvent(SwitchEvent)

    verify {
        callbacks.onTriggeredTransition(SwitchEvent)
        callbacks.onExitState(outerState1)
        if (!startInnerMachineOnSetup) {
            callbacks.onStarted(innerMachine)
            callbacks.onEntryState(innerMachine)
            callbacks.onEntryState(innerState1)
        }
    }
    clearMocks(callbacks, answers = false)

    innerMachine.processEvent(SwitchEvent)

    verifyOrder {
        callbacks.onTriggeredTransition(SwitchEvent)
        callbacks.onExitState(innerState1)
        callbacks.onEntryState(innerState2)
    }
    clearMocks(callbacks, answers = false)
}