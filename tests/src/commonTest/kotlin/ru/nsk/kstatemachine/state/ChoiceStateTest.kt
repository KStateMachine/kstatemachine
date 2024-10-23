/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.verifySequence
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.event.DataEvent
import ru.nsk.kstatemachine.event.defaultDataExtractor
import ru.nsk.kstatemachine.state.ChoiceStateTestData.IntEvent
import ru.nsk.kstatemachine.state.ChoiceStateTestData.State1
import ru.nsk.kstatemachine.state.ChoiceStateTestData.State2
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.onTransitionTriggered
import ru.nsk.kstatemachine.statemachine.processEventBlocking

private object ChoiceStateTestData {
    object State1 : DefaultState()
    object State2 : DefaultState()

    class IntEvent(override val data: Int) : DataEvent<Int>
}

class ChoiceStateTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "redirecting choice state" {
            val callbacks = mockkCallbacks()

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it()) }

                val choice = choiceState("choice") {
                    log { "$event $argument" }
                    State2
                }

                addInitialState(State1) {
                    transition<SwitchEvent> { targetState = choice }
                }
                addState(State2) { callbacks.listen(this) }
                onTransitionTriggered { log { it.toString() } }
            }

            machine.processEventBlocking(SwitchEvent, false)

            verifySequence { callbacks.onStateEntry(State2) }
        }

        "redirecting choice states chain" {
            val callbacks = mockkCallbacks()

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it()) }

                val choice2 = choiceState("choice2") { State2 }
                val choice1 = choiceState("choice1") { choice2 }

                addInitialState(State1) {
                    transition<SwitchEvent> { targetState = choice1 }
                }
                addState(State2) { callbacks.listen(this) }
            }

            machine.processEventBlocking(SwitchEvent)

            verifySequence { callbacks.onStateEntry(State2) }
        }

        "initial choiceState" {
            val callbacks = mockkCallbacks()

            createTestStateMachine(coroutineStarterType) {
                initialChoiceState("choice") { State2 }
                addState(State2) { callbacks.listen(this) }
            }
            verifySequence { callbacks.onStateEntry(State2) }
        }

        "initial choiceDataState" {
            val callbacks = mockkCallbacks()
            lateinit var state2: DataState<Int>
            createTestStateMachine(coroutineStarterType) {
                initialChoiceDataState("choice") { state2 }
                state2 = dataState(defaultData = 42) { callbacks.listen(this) }
            }
            verifySequence { callbacks.onStateEntry(state2) }
            state2.data shouldBe 42
        }

        "initial choice state on entry parent" {
            lateinit var state1: State
            lateinit var state2: State
            val machine = createTestStateMachine(coroutineStarterType) {
                state1 = initialState("state1") {
                    initialChoiceState("choice") { state2 }
                }

                state2 = state("state2") {
                    transition<SwitchEvent>(targetState = state1)
                }
            }
            machine.processEvent(SwitchEvent)
            machine.activeStates().shouldContainExactly(state2)
        }

        "initial choice state in a parallel state" {
            lateinit var state2: State
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState("state1", childMode = ChildMode.PARALLEL) {
                    state("state11") {}
                    state("state12") {
                        initialChoiceState("choice") { state2 }
                    }
                }

                state2 = state("state2")
            }
            machine.activeStates().shouldContainExactly(state2)
        }

        "redirecting choice data state" {
            val callbacks = mockkCallbacks()

            class IntEvent(override val data: Int) : DataEvent<Int>

            lateinit var intState1: DataState<Int>
            lateinit var intState2: DataState<Int>

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it()) }

                addInitialState(State1)

                val choice = choiceDataState("data choice") {
                    log { "$event $argument" }
                    val intEvent = event as? IntEvent // cast is necessary as we don't know event type here
                    if (intEvent?.data == 42) intState1 else intState2
                }

                dataTransition<IntEvent, Int> { targetState = choice }

                intState1 = dataState<Int>("intState1") { callbacks.listen(this) }
                intState2 = dataState<Int>("intState2") { callbacks.listen(this) }
                onTransitionTriggered { log { it.toString() } }
            }

            machine.processEvent(IntEvent(42), true)
            verifySequenceAndClear(callbacks) { callbacks.onStateEntry(intState1) }
            machine.processEvent(IntEvent(66), false)
            verifySequenceAndClear(callbacks) {
                callbacks.onStateExit(intState1)
                callbacks.onStateEntry(intState2)
            }
        }


        "Try reproduce https://github.com/KStateMachine/kstatemachine/issues/101" {
            lateinit var state3: State
            val machine = createTestStateMachine(coroutineStarterType) {
                state3 = state("state3")
                val state2 = dataState<Int>("dataState3") {
                    initialChoiceState { state3 }
                }
                initialState("state1") {
                    dataTransition<IntEvent, Int> { targetState = state2 }
                }
            }
            machine.processEvent(IntEvent(42))
            machine.activeStates().shouldContainExactly(state3)
        }
    }
})