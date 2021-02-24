package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

private object State1 : DefaultState("state1")
private object State2 : DefaultState("state2")

/**
 * States are mutable and currently it is not possible to use object states in multiple [StateMachine] instances.
 * May be it might be partly fixed with moving mutated state part into [StateMachine] (some graph object),
 * but it will not completely protect from wrong usage (for example [State] subclasses may hold some mutable data).
 */
class ObjectStatesTest {
    @Test
    fun multipleUsageOfObjectStates() {
        useInMachine()
        shouldThrow<IllegalStateException> {
            useInMachine()
        }
    }

    private fun useInMachine() {
        val machine = createStateMachine {
            addInitialState(State1) {
                transition<SwitchEvent> {
                    targetState = State2
                }
            }
            addState(State2)
        }

        machine.processEvent(SwitchEvent)
    }
}