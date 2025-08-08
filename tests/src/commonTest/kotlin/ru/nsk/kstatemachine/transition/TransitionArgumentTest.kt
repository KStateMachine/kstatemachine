/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.transition

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.verifySequence
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.processEventBlocking
import ru.nsk.kstatemachine.statemachine.startBlocking

private const val ARGUMENT = 1

class TransitionArgumentTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
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

                machine.processEventBlocking(SwitchEvent)
                verifySequence { callbacks.onStateEntry(second) }
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
                machine.startBlocking(ARGUMENT)

                machine.processEventBlocking(SwitchEvent)
                verifySequence { callbacks.onStateEntry(state1) }
            }
        }
    }
})