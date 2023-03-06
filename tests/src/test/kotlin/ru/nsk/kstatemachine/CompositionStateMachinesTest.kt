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
    CoroutineStarterType.values().forEach { coroutineStarterType ->
        "composition, inner machine auto start on entry" { composition(coroutineStarterType, false) }
        "composition, inner machine already started" { composition(coroutineStarterType, true) }

        "nested machine as initial state" {
            val callbacks = mockkCallbacks()
            lateinit var state1: IState
            val inner = createTestStateMachine(coroutineStarterType, start = false) {
                state1 = initialState("state1") {
                    callbacks.listen(this)
                }
            }

            createTestStateMachine(coroutineStarterType) {
                addInitialState(inner)
            }

            verifySequence {
                callbacks.onEntryState(state1)
            }
        }

        "stop nested machines" {
            val callbacks = mockkCallbacks()
            val inner = createTestStateMachine(coroutineStarterType) {
                initialState("state1") {
                    callbacks.listen(this)
                }
            }

            val outer = createTestStateMachine(coroutineStarterType) {
                addInitialState(inner)
            }

            outer.isActive shouldBe true
            inner.isActive shouldBe true
            outer.stopBlocking()
            outer.isActive shouldBe false
            inner.isActive shouldBe true
        }

        "exit branch with nested machine" {
            val callbacks = mockkCallbacks()

            val inner = createTestStateMachine(coroutineStarterType) {
                initialState { callbacks.listen(this) }
            }

            lateinit var state1: State
            lateinit var state2: State
            val outer = createTestStateMachine(coroutineStarterType) {
                state1 = initialState {
                    addInitialState(inner)
                    transitionOn<SwitchEvent> { targetState = { state2 } }
                }
                state2 = state("state2")
            }

            outer.activeStates().shouldContainExactly(state1, inner)

            outer.processEventBlocking(SwitchEvent)
            outer.isActive shouldBe true
            inner.isActive shouldBe true
            outer.activeStates().shouldContainExactly(state2)
        }

        "transition out from nested machine, negative" {
            lateinit var state1: State
            val inner = createTestStateMachine(coroutineStarterType, name = "inner") {
                logger = StateMachine.Logger { println(it()) }
                state1 = initialState("inner-state1")
            }

            lateinit var state2: State
            val outer = createTestStateMachine(coroutineStarterType, name = "outer") {
                logger = StateMachine.Logger { println(it()) }

                addInitialState(inner) {
                    transitionOn<SwitchEvent> { targetState = { state2 } } // invalid
                }
                state2 = state("outer-state2")
            }

            inner.activeStates().shouldContainExactly(state1)

            outer.processEventBlocking(SwitchEvent) // ignored
            inner.activeStates().shouldContainExactly(state1)
            outer.activeStates().shouldContainExactly(inner)
        }

        "transition into nested machine sub-state, negative" {
            lateinit var innerState2: State
            val inner = createTestStateMachine(coroutineStarterType, name = "inner") {
                logger = StateMachine.Logger { println(it()) }
                initialState("inner-state1")
                innerState2 = state("inner-state2")
            }

            val outer = createTestStateMachine(coroutineStarterType, name = "outer") {
                logger = StateMachine.Logger { println(it()) }

                initialState("state1") {
                    transitionOn<SwitchEvent> { targetState = { innerState2 } } // invalid
                }
                addState(inner)
            }

            shouldThrow<IllegalStateException> { outer.processEventBlocking(SwitchEvent) }
        }
    }
})

private fun composition(coroutineStarterType: CoroutineStarterType, startInnerMachineOnSetup: Boolean) {
    val callbacks = mockkCallbacks()

    val outerState1 = DefaultState("Outer state1")
    val innerState1 = DefaultState("Inner state1")
    val innerState2 = DefaultState("Inner state2")

    val innerMachine = createTestStateMachine(
        coroutineStarterType,
        name = "Inner machine",
        start = startInnerMachineOnSetup
    ) {
        logger = StateMachine.Logger { println(it()) }

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

    val machine = createTestStateMachine(coroutineStarterType) {
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

    machine.processEventBlocking(SwitchEvent)

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

    innerMachine.processEventBlocking(SwitchEvent)

    verifyOrder {
        callbacks.onTriggeredTransition(SwitchEvent)
        callbacks.onExitState(innerState1)
        callbacks.onEntryState(innerState2)
    }
    clearMocks(callbacks, answers = false)
}