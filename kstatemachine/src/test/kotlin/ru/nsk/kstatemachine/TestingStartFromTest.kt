package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.mockk.verifySequence
import ru.nsk.kstatemachine.Testing.startFrom

class TestingStartFromTest : StringSpec({
    "startFrom()" {
        val callbacks = mockkCallbacks()

        lateinit var state2: State
        lateinit var state22: State

        val machine = createStateMachine(start = false) {
            callbacks.listen(this)

            initialState("state1") { callbacks.listen(this) }
            state2 = state("state2") {
                callbacks.listen(this)

                initialState("state2_1") { callbacks.listen(this) }
                state22 = state("state2_2") { callbacks.listen(this) }
            }

            onStarted { callbacks.onStarted(this) }
        }

        machine.startFrom(state22)

        verifySequence {
            callbacks.onStarted(machine)
            callbacks.onEntryState(machine)
            callbacks.onEntryState(state2)
            callbacks.onEntryState(state22)
        }
    }
})