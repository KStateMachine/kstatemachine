package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.verifySequence

private const val ARGUMENT = 1

class TransitionArgumentTest : StringSpec({
    CoroutineStarterType.values().forEach { coroutineStarterType ->
        "transition argument" {
            val callbacks = mockkCallbacks()

            val second = object : DefaultState("second") {}

            val machine = createTestStateMachine(coroutineStarterType) {
                addState(second) {
                    callbacks.listen(this)
                    onEntry { it.transition.argument shouldBe ARGUMENT }
                }
                initialState("first") {
                    transition<SwitchEvent> {
                        targetState = second
                        onTriggered { it.transition.argument = ARGUMENT }
                    }
                }
            }

            machine.processEvent(SwitchEvent)
            verifySequence { callbacks.onEntryState(second) }
        }

        "transition argument on start" {
            val callbacks = mockkCallbacks()
            lateinit var state1: State

            val machine = createTestStateMachine(coroutineStarterType, start = false) {
                state1 = initialState("first") {
                    callbacks.listen(this)
                    onEntry { it.argument shouldBe ARGUMENT }
                }
            }
            machine.start(ARGUMENT)

            machine.processEvent(SwitchEvent)
            verifySequence { callbacks.onEntryState(state1) }
        }
    }
})