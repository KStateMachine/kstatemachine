/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import ru.nsk.kstatemachine.CoroutineStarterType
import ru.nsk.kstatemachine.SwitchEvent
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.state.ObjectStatesTestData.State1
import ru.nsk.kstatemachine.state.ObjectStatesTestData.State2
import ru.nsk.kstatemachine.statemachine.*

private object ObjectStatesTestData {
    object State1 : DefaultState("state1")
    object State2 : DefaultState("state2")
}

/**
 * States are mutable, and it is not possible to use object states in multiple [StateMachine] instances if
 * [CreationArguments.autoDestroyOnStatesReuse] argument is false.
 */
class ObjectStatesTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "multiple usage of object states throws" {
            val machine = useInMachine(coroutineStarterType, false)
            shouldThrow<IllegalStateException> { useInMachine(coroutineStarterType, false) }
            shouldThrow<IllegalStateException> { useInMachine(coroutineStarterType, true) }
            machine.destroyBlocking()
        }

        "multiple usage of object states allowed" {
            useInMachine(coroutineStarterType, true)
            useInMachine(coroutineStarterType, true).destroyBlocking()
        }

        "multiple usage of object states allowed first machine stopped" {
            useInMachine(coroutineStarterType, true).stop()
            useInMachine(coroutineStarterType, true).destroyBlocking()
        }

        "multiple usage of object states throws if current machine forbids auto destroy" {
            useInMachine(coroutineStarterType, true)
            val machine = useInMachine(coroutineStarterType, false)
            shouldThrow<IllegalStateException> { useInMachine(coroutineStarterType, true) }
            machine.destroyBlocking()
        }

        "multiple usage of object states allowed with manual calling destroy()" {
            useInMachine(coroutineStarterType, false).destroyBlocking()
            useInMachine(coroutineStarterType, false).destroyBlocking()
        }
    }
})

private fun useInMachine(coroutineStarterType: CoroutineStarterType, autoDestroyOnStatesReuse: Boolean): StateMachine {
    val machine = createTestStateMachine(
        coroutineStarterType,
        creationArguments = buildCreationArguments { this.autoDestroyOnStatesReuse = autoDestroyOnStatesReuse },
    ) {
        addInitialState(State1) {
            transition<SwitchEvent> {
                targetState = State2
            }
        }
        addState(State2)
    }

    machine.processEventBlocking(SwitchEvent)
    return machine
}