package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import ru.nsk.kstatemachine.StateCleanupTestData.State1

private object StateCleanupTestData {
    class State1 : DefaultState("state1")
}

class StateCleanupTest : StringSpec({
    CoroutineStarterType.values().forEach { coroutineStarterType ->
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
})

private fun useInMachine(coroutineStarterType: CoroutineStarterType, state: IState) = createTestStateMachine(coroutineStarterType) {
    addInitialState(state)
}