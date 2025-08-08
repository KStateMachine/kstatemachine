/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import io.kotest.core.spec.style.FreeSpec
import ru.nsk.kstatemachine.CoroutineStarterType
import ru.nsk.kstatemachine.SwitchEvent
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.onStarted
import ru.nsk.kstatemachine.statemachine.processEventBlocking
import ru.nsk.kstatemachine.transition.Transition
import ru.nsk.kstatemachine.transition.TransitionParams
import ru.nsk.kstatemachine.transition.onTriggered

class ListenersModificationTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "remove state listener from callback" {
            createTestStateMachine(coroutineStarterType) {
                initialState {
                    onEntry(once = true) {

                    }
                    onEntry { /* nothing */ }
                }
            }
        }

        "remove transition listener from callback" {
            val machine = createTestStateMachine(coroutineStarterType) {
                val state2 = state()
                initialState {
                    val transition = transition<SwitchEvent> {
                        targetState = state2
                        @Suppress("UNUSED_VARIABLE")
                        val checkListenerIsReturned = onTriggered { /* removing listener from DSL is very verbose */ }
                    }
                    transition.addListener(object : Transition.Listener {
                        override suspend fun onTriggered(transitionParams: TransitionParams<*>) {
                            transition.removeListener(this)
                        }
                    })
                    transition.onTriggered { /* nothing, just fill listeners collection */ }
                }
            }
            machine.processEventBlocking(SwitchEvent)
        }

        "remove machine listener from callback" {
            createTestStateMachine(coroutineStarterType) {
                initialState()
                lateinit var listener: StateMachine.Listener
                listener = onStarted { removeListener(listener) }
                onStarted { /* nothing */ }
            }
        }
    }
})