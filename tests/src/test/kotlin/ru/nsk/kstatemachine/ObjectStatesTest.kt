package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import ru.nsk.kstatemachine.ObjectStatesTestData.State1
import ru.nsk.kstatemachine.ObjectStatesTestData.State2

private object ObjectStatesTestData {
    object State1 : DefaultState("state1")
    object State2 : DefaultState("state2")
}

/**
 * States are mutable, and it is not possible to use object states in multiple [StateMachine] instances if
 * autoDestroyOnStatesReuse argument is false.
 */
class ObjectStatesTest : StringSpec({
    "multiple usage of object states throws" {
        val machine = useInMachine(false)
        shouldThrow<IllegalStateException> { useInMachine(false) }
        shouldThrow<IllegalStateException> { useInMachine(true) }
        machine.destroy()
    }

    "multiple usage of object states allowed" {
        useInMachine(true)
        useInMachine(true).destroy()
    }

    "multiple usage of object states throws if current machine forbids auto destroy" {
        useInMachine(true)
        val machine = useInMachine(false)
        shouldThrow<IllegalStateException> { useInMachine(true) }
        machine.destroy()
    }

    "multiple usage of object states allowed with manual calling destroy()" {
        useInMachine(false).destroy()
        useInMachine(false).destroy()
    }
})

private fun useInMachine(autoDestroyOnStatesReuse: Boolean): StateMachine {
    val machine = createStateMachine(autoDestroyOnStatesReuse = autoDestroyOnStatesReuse) {
        addInitialState(State1) {
            transition<SwitchEvent> {
                targetState = State2
            }
        }
        addState(State2)
    }

    machine.processEvent(SwitchEvent)
    return machine
}