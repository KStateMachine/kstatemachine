/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.mockk.called
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.state.StateGroupListenerTestData.State1
import ru.nsk.kstatemachine.state.StateGroupListenerTestData.State2
import ru.nsk.kstatemachine.state.StateGroupListenerTestData.State3
import ru.nsk.kstatemachine.statemachine.processEventBlocking
import ru.nsk.kstatemachine.statemachine.startBlocking

private object StateGroupListenerTestData {
    object State1 : DefaultState()
    object State2 : DefaultState()
    object State3 : DefaultState()
}

class StateGroupListenerTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "onActiveAllOf()" - {
            withData(
                { "callback behaviour with notifyOnSubscribe:$it" },
                first = true, second = false
            ) { notifyOnSubscribe ->
                val callback = mockk<Callback<Boolean>>(relaxed = true)

                createTestStateMachine(coroutineStarterType) {
                    addInitialState(State1)
                    addState(State2)
                    addState(State3)
                }

                onActiveAllOf(State1, State2, State3, notifyOnSubscribe = notifyOnSubscribe) { callback(it) }

                verifySequence(inverse = !notifyOnSubscribe) { callback(false) }
            }

            "all active on subscribe" {
                val callback = mockk<Callback<Boolean>>(relaxed = true)

                createTestStateMachine(coroutineStarterType, childMode = ChildMode.PARALLEL) {
                    addState(State1)
                    addState(State2)
                    addState(State3)
                }

                onActiveAllOf(State1, State2, State3, notifyOnSubscribe = true) { callback(it) }

                verifySequence { callback(true) }
            }

            "callback called on isActive changes" {
                val callback = mockk<Callback<Boolean>>(relaxed = true)

                val machine = createTestStateMachine(coroutineStarterType) {
                    addInitialState(State1) {
                        transition<SwitchEvent>(targetState = State2)
                    }
                    addState(State2) {
                        transition<SwitchEvent>(targetState = State3)
                    }
                    addState(State3)

                    onActiveAllOf(machine, State2, notifyOnSubscribe = true) { callback(it) }
                }
                machine.processEventBlocking(SwitchEvent)
                machine.processEventBlocking(SwitchEvent)

                verifySequence {
                    callback(false)
                    callback(true)
                    callback(false)
                }
            }

            "use with states of different machines" {
                val callback = mockk<Callback<Boolean>>(relaxed = true)

                createTestStateMachine(coroutineStarterType) {
                    addInitialState(State1)
                }
                createTestStateMachine(coroutineStarterType) {
                    addInitialState(State2)
                }

                onActiveAllOf(State1, State2, notifyOnSubscribe = true) { callback(it) }

                verifySequence { callback(true) }
            }

            "unsubscribe" {
                val callback = mockk<Callback<Boolean>>(relaxed = true)

                val machine = createTestStateMachine(coroutineStarterType, start = false) {
                    addInitialState(State1) {
                        transition<SwitchEvent>(targetState = State2)
                    }
                    addState(State2)
                }

                val listener = onActiveAllOf(machine, State1) { callback(it) }
                machine.startBlocking()

                verifySequenceAndClear(callback) { callback(true) }

                listener.unsubscribe()
                machine.processEventBlocking(SwitchEvent)

                verify { callback wasNot called }
            }
        }

        "onActiveAnyOf()" - {
            withData(
                { "callback behaviour with notifyOnSubscribe:$it" },
                first = true, second = false
            ) { notifyOnSubscribe ->
                val callback = mockk<Callback<Boolean>>(relaxed = true)

                createTestStateMachine(coroutineStarterType) {
                    addInitialState(State1)
                    addState(State2)
                    addState(State3)
                }

                onActiveAnyOf(State1, State2, State3, notifyOnSubscribe = notifyOnSubscribe) { callback(it) }

                verifySequence(inverse = !notifyOnSubscribe) { callback(true) }
            }

            "callback called on isActive changes" {
                val callback = mockk<Callback<Boolean>>(relaxed = true)

                val machine = createTestStateMachine(coroutineStarterType) {
                    addInitialState(State1) {
                        transition<SwitchEvent>(targetState = State2)
                    }
                    addState(State2) {
                        transition<SwitchEvent>(targetState = State3)
                    }
                    addState(State3)

                    onActiveAnyOf(State1, State3) { callback(it) }
                }
                machine.processEventBlocking(SwitchEvent)
                machine.processEventBlocking(SwitchEvent)

                verifySequence {
                    callback(true)
                    callback(false)
                    callback(true)
                }
            }

            "callback is called on switching between active states, as switching is not atomic" {
                val callback = mockk<Callback<Boolean>>(relaxed = true)

                val machine = createTestStateMachine(coroutineStarterType) {
                    addInitialState(State1) {
                        transition<SwitchEvent>(targetState = State2)
                    }
                    addState(State2)

                    onActiveAnyOf(State1, State2) { callback(it) }
                }
                machine.processEventBlocking(SwitchEvent)

                verifySequence {
                    callback(true)
                    callback(false)
                    callback(true)
                }
            }

            "use with states of different machines" {
                val callback = mockk<Callback<Boolean>>(relaxed = true)

                createTestStateMachine(coroutineStarterType) {
                    addInitialState(State1)
                }
                createTestStateMachine(coroutineStarterType) {
                    addInitialState(State2)
                }

                onActiveAnyOf(State1, State2, notifyOnSubscribe = true) { callback(it) }

                verifySequence { callback(true) }
            }

            "unsubscribe" {
                val callback = mockk<Callback<Boolean>>(relaxed = true)

                val machine = createTestStateMachine(coroutineStarterType, start = false) {
                    addInitialState(State1) {
                        transition<SwitchEvent>(targetState = State2)
                    }
                    addState(State2)
                    addState(State3)
                }

                val listener = onActiveAnyOf(State1, State3) { callback(it) }
                machine.startBlocking()

                verifySequenceAndClear(callback) { callback(true) }

                listener.unsubscribe()
                machine.processEventBlocking(SwitchEvent)

                verify { callback wasNot called }
            }
        }

        "onActiveAllOf()/onActiveAnyOf() invalid set of states" {
            createTestStateMachine(coroutineStarterType) {
                addInitialState(State1)
                addState(State2)
            }

            shouldThrow<IllegalArgumentException> { onActiveAllOf(State1, State1, State1) { /*nothing*/ } }
            shouldThrow<IllegalArgumentException> { onActiveAnyOf(State1, State1, State1) { /*nothing*/ } }
        }
    }
})