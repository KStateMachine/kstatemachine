package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec

class ListenersModificationTest : StringSpec({
    "remove state listener from callback" {
        createStateMachine {
            initialState {
                onEntry(once = true) {

                }
                onEntry { /* nothing */ }
            }
        }
    }

    "remove transition listener from callback" {
        val machine = createStateMachine {
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
        machine.processEvent(SwitchEvent)
    }

    "remove machine listener from callback" {
        createStateMachine {
            initialState()
            lateinit var listener: StateMachine.Listener
            listener = onStarted { removeListener(listener) }
            onStarted { /* nothing */ }
        }
    }
})