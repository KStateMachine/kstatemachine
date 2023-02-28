package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.mockk.called
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import ru.nsk.kstatemachine.StateGroupListenerTestData.State1
import ru.nsk.kstatemachine.StateGroupListenerTestData.State2
import ru.nsk.kstatemachine.StateGroupListenerTestData.State3

private object StateGroupListenerTestData {
    object State1 : DefaultState()
    object State2 : DefaultState()
    object State3 : DefaultState()
}

class StateGroupListenerTest : FreeSpec({
    CoroutineStarterType.values().forEach { coroutineStarterType ->
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
                machine.processEvent(SwitchEvent)
                machine.processEvent(SwitchEvent)

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
                machine.start()

                verifySequenceAndClear(callback) { callback(true) }

                listener.unsubscribe()
                machine.processEvent(SwitchEvent)

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
                machine.processEvent(SwitchEvent)
                machine.processEvent(SwitchEvent)

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
                machine.processEvent(SwitchEvent)

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
                machine.start()

                verifySequenceAndClear(callback) { callback(true) }

                listener.unsubscribe()
                machine.processEvent(SwitchEvent)

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