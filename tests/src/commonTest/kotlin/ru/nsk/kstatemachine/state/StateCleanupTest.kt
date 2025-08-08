/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.spyk
import ru.nsk.kstatemachine.CoroutineStarterType
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.state.StateCleanupTestData.State1
import ru.nsk.kstatemachine.statemachine.destroyBlocking

private object StateCleanupTestData {
    class State1 : DefaultState("state1")
}

class StateCleanupTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "cleanup is not called" {
                val state = spyk<State1>()
                useInMachine(coroutineStarterType, state)
                coVerify(inverse = true) { state.onCleanup() }
            }

            "cleanup is called on machine manual destruction" {
                val state = spyk<State1>()
                useInMachine(coroutineStarterType, state).destroyBlocking()
                coVerify(exactly = 1) { state.onCleanup() }
            }

            "cleanup is called on machine auto destruction" {
                val state = spyk<State1>()
                val machine1 = useInMachine(coroutineStarterType, state)
                val machine2 = useInMachine(coroutineStarterType, state)

                coVerify(exactly = 1) { state.onCleanup() }
                machine1.isDestroyed shouldBe true
                machine2.isDestroyed shouldBe false
            }
        }
    }
})

private suspend fun useInMachine(coroutineStarterType: CoroutineStarterType, state: IState) =
    createTestStateMachine(coroutineStarterType) {
        addInitialState(state)
    }