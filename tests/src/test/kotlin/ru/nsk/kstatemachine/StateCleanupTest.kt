package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.mockk.mockkObject
import io.mockk.verify
import ru.nsk.kstatemachine.StateMachineDestroyTestData.State1
import ru.nsk.kstatemachine.StateMachineDestroyTestData.useInMachine

private object StateMachineDestroyTestData {
    object State1 : DefaultState("state1")

    fun useInMachine() = createTestStateMachine {
        addInitialState(State1)
    }
}

class StateCleanupTest : StringSpec({
    "cleanup is not called" {
        mockkObject(State1) {
            val machine = useInMachine()
            try {
                verify(inverse = true) { State1.onCleanup() }
            } finally {
                machine.destroy()
            }
        }
    }

    "cleanup is called on machine manual destruction" {
        mockkObject(State1) {
            useInMachine().destroy()

            verify(exactly = 1) { State1.onCleanup() }
        }
    }

    "cleanup is called on machine auto destruction" {
        mockkObject(State1) {
            useInMachine()
            useInMachine()

            verify(exactly = 1) { State1.onCleanup() }
        }
    }
})