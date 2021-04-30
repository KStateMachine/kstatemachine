package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import org.junit.jupiter.api.Test


/**
 * In a parent state machine it is not possible to use as transitions targets states from inner machine and vise versa.
 * Inner machine is treated as atomic state by outer one.
 * Inner machine is started automatically when outer one enters it.
 */
class CompositionStateMachinesTest {
    @Test
    fun compositionInnerAutoStart() = composition(false)

    @Test
    fun compositionInnerManualStart() = composition(true)
}

private fun composition(startInnerMachineOnSetup: Boolean) {
    val callbacks = mock<Callbacks>()

    val outerState1 = DefaultState("Outer state1")
    val innerState1 = DefaultState("Inner state1")
    val innerState2 = DefaultState("Inner state2")

    val innerMachine = createStateMachine("Inner machine", startInnerMachineOnSetup) {
        callbacks.listen(this)

        onStarted {
            callbacks.onStarted(this)
        }

        addInitialState(innerState1) {
            callbacks.listen(this)

            transition<SwitchEvent> {
                targetState = innerState2

                callbacks.listen(this)
            }
        }

        addState(innerState2) {
            callbacks.listen(this)
        }
    }

    val machine = createStateMachine {
        callbacks.listen(this)

        addInitialState(outerState1) {
            callbacks.listen(this)

            transition<SwitchEvent> {
                targetState = innerMachine

                callbacks.listen(this)
            }
        }

        addState(innerMachine)
    }

    then(callbacks).should().onEntryState(outerState1)

    machine.processEvent(SwitchEvent)

    then(callbacks).should().onStarted(innerMachine)
    then(callbacks).should().onEntryState(innerMachine)
    then(callbacks).should().onEntryState(innerState1)

    innerMachine.processEvent(SwitchEvent)

    then(callbacks).should().onExitState(innerState1)
    then(callbacks).should().onEntryState(innerState2)
}