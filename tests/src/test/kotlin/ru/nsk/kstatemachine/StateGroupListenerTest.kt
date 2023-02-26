package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence

class StateGroupListenerTest : FreeSpec({
    "onActiveAllOf()" - {
        withData(
            { "callback behaviour with notifyOnSubscribe:$it" },
            first = true, second = false
        ) { notifyOnSubscribe ->
            val callback = mockk<Callback<Boolean>>(relaxed = true)

            createStateMachine {
                addInitialState(State1)
                addState(State2)
                addState(State3)
            }

            onActiveAllOf(State1, State2, State3, notifyOnSubscribe = notifyOnSubscribe) { callback.invoke(it) }

            verify(inverse = !notifyOnSubscribe, exactly = 1) { callback.invoke(false) }
        }

        "all active on subscribe" {
            val callback = mockk<Callback<Boolean>>(relaxed = true)

            createStateMachine(childMode = ChildMode.PARALLEL) {
                addState(State1)
                addState(State2)
                addState(State3)
            }

            onActiveAllOf(State1, State2, State3, notifyOnSubscribe = true) { callback.invoke(it) }

            verifySequence { callback.invoke(true) }
        }

        "callback called on isActive changes" {
            val callback = mockk<Callback<Boolean>>(relaxed = true)

            val machine = createStateMachine {
                addInitialState(State1) {
                    transition<SwitchEvent>(targetState = State2)
                }
                addState(State2) {
                    transition<SwitchEvent>(targetState = State3)
                }
                addState(State3)

                onActiveAllOf(machine, State2, notifyOnSubscribe = true) { callback.invoke(it) }
            }
            machine.processEvent(SwitchEvent)
            machine.processEvent(SwitchEvent)

            verifySequence {
                callback.invoke(false)
                callback.invoke(true)
                callback.invoke(false)
            }
        }

        "use with states of different machines" {
            val callback = mockk<Callback<Boolean>>(relaxed = true)

            createStateMachine {
                addInitialState(State1)
            }
            createStateMachine {
                addInitialState(State2)
            }

            onActiveAllOf(State1, State2, notifyOnSubscribe = true) { callback.invoke(it) }

            verifySequence { callback.invoke(true) }
        }

        "unsubscribe" {
            val callback = mockk<Callback<Boolean>>(relaxed = true)

            val machine = createStateMachine(start = false) {
                addInitialState(State1) {
                    transition<SwitchEvent>(targetState = State2)
                }
                addState(State2)
            }

            val listener = onActiveAllOf(machine, State1) { callback.invoke(it) }
            machine.start()

            verifySequence { callback.invoke(true) }

            listener.unsubscribe()
            machine.processEvent(SwitchEvent)

            verify(inverse = true) { callback.invoke(false) }
        }
    }

    "onActiveAnyOf()" - {
        withData(
            { "callback behaviour with notifyOnSubscribe:$it" },
            first = true, second = false
        ) { notifyOnSubscribe ->
            val callback = mockk<Callback<Boolean>>(relaxed = true)

            createStateMachine {
                addInitialState(State1)
                addState(State2)
                addState(State3)
            }

            onActiveAnyOf(State1, State2, State3, notifyOnSubscribe = notifyOnSubscribe) { callback.invoke(it) }

            verify(inverse = !notifyOnSubscribe, exactly = 1) { callback.invoke(true) }
        }

        "callback called on isActive changes" {
            val callback = mockk<Callback<Boolean>>(relaxed = true)

            val machine = createStateMachine {
                addInitialState(State1) {
                    transition<SwitchEvent>(targetState = State2)
                }
                addState(State2) {
                    transition<SwitchEvent>(targetState = State3)
                }
                addState(State3)

                onActiveAnyOf(State1, State3) { callback.invoke(it) }
            }
            machine.processEvent(SwitchEvent)
            machine.processEvent(SwitchEvent)

            verifySequence {
                callback.invoke(true)
                callback.invoke(false)
                callback.invoke(true)
            }
        }

        "callback is called on switching between active states, as switching is not atomic" {
            val callback = mockk<Callback<Boolean>>(relaxed = true)

            val machine = createStateMachine {
                addInitialState(State1) {
                    transition<SwitchEvent>(targetState = State2)
                }
                addState(State2)

                onActiveAnyOf(State1, State2) { callback.invoke(it) }
            }
            machine.processEvent(SwitchEvent)

            verifySequence {
                callback.invoke(true)
                callback.invoke(false)
                callback.invoke(true)
            }
        }

        "use with states of different machines" {
            val callback = mockk<Callback<Boolean>>(relaxed = true)

            createStateMachine {
                addInitialState(State1)
            }
            createStateMachine {
                addInitialState(State2)
            }

            onActiveAnyOf(State1, State2, notifyOnSubscribe = true) { callback.invoke(it) }

            verifySequence { callback.invoke(true) }
        }

        "unsubscribe" {
            val callback = mockk<Callback<Boolean>>(relaxed = true)

            val machine = createStateMachine(start = false) {
                addInitialState(State1) {
                    transition<SwitchEvent>(targetState = State2)
                }
                addState(State2)
                addState(State3)
            }

            val listener = onActiveAnyOf(State1, State3) { callback.invoke(it) }
            machine.start()

            verifySequence { callback.invoke(true) }

            listener.unsubscribe()
            machine.processEvent(SwitchEvent)

            verify(inverse = true) { callback.invoke(false) }
        }
    }

    "onActiveAllOf()/onActiveAnyOf() invalid set of states" {
        createStateMachine {
            addInitialState(State1)
            addState(State2)
        }

        shouldThrow<IllegalArgumentException> { onActiveAllOf(State1, State1, State1) { /*nothing*/ } }
        shouldThrow<IllegalArgumentException> { onActiveAnyOf(State1, State1, State1) { /*nothing*/ } }
    }
}) {
    private object State1 : DefaultState()
    private object State2 : DefaultState()
    private object State3 : DefaultState()
}