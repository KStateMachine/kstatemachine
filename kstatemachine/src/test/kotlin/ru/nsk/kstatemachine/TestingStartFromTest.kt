package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
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

                initialState("state21") { callbacks.listen(this) }
                state22 = state("state22") { callbacks.listen(this) }
            }

            onStarted { callbacks.onStarted(this) }
        }

        machine.startFrom(state22)

        verifySequenceAndClear(callbacks) {
            callbacks.onStarted(machine)
            callbacks.onEntryState(machine)
            callbacks.onEntryState(state2)
            callbacks.onEntryState(state22)
        }

        machine.stop()
        machine.activeStates().shouldBeEmpty()

        machine.startFrom("state22")

        verifySequenceAndClear(callbacks) {
            callbacks.onStarted(machine)
            callbacks.onEntryState(machine)
            callbacks.onEntryState(state2)
            callbacks.onEntryState(state22)
        }
    }
})