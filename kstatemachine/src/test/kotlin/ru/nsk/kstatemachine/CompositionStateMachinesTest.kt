package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.mockk.clearMocks
import io.mockk.verify
import io.mockk.verifyOrder

/**
 * In a parent state machine it is not possible to use as transitions targets states from inner machine and vise versa.
 * Inner machine is treated as atomic state by outer one.
 * Inner machine is started automatically when outer one enters it.
 */
class CompositionStateMachinesTest : StringSpec({
    "composition, inner machine auto start on entry" { composition(false) }
    "composition, inner machine already started" { composition(true) }
})


private fun composition(startInnerMachineOnSetup: Boolean) {
    val callbacks = mockkCallbacks()

    val outerState1 = DefaultState("Outer state1")
    val innerState1 = DefaultState("Inner state1")
    val innerState2 = DefaultState("Inner state2")

    val innerMachine = createStateMachine("Inner machine", start = startInnerMachineOnSetup) {
        logger = StateMachine.Logger { println(it) }

        callbacks.listen(this)

        onStarted {
            callbacks.onStarted(this)
        }

        addInitialState(innerState1) {
            callbacks.listen(this)

            transition<SwitchEvent>("Switch") {
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

    verifyOrder {
        callbacks.onEntryState(machine)
        callbacks.onEntryState(outerState1)
    }
    clearMocks(callbacks, answers = false)

    machine.processEvent(SwitchEvent)

    verify {
        callbacks.onTriggeredTransition(SwitchEvent)
        callbacks.onExitState(outerState1)
        if (!startInnerMachineOnSetup) {
            callbacks.onStarted(innerMachine)
            callbacks.onEntryState(innerMachine)
            callbacks.onEntryState(innerState1)
        }
    }
    clearMocks(callbacks, answers = false)

    innerMachine.processEvent(SwitchEvent)

    verifyOrder {
        callbacks.onTriggeredTransition(SwitchEvent)
        callbacks.onExitState(innerState1)
        callbacks.onEntryState(innerState2)
    }
    clearMocks(callbacks, answers = false)
}