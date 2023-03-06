package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec

class ListenersModificationTest : StringSpec({
    CoroutineStarterType.values().forEach { coroutineStarterType ->
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