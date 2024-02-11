package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ru.nsk.kstatemachine.Testing.startFrom
import ru.nsk.kstatemachine.Testing.startFromBlocking

class TestingStartFromTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "startFromBlocking()" {
            val callbacks = mockkCallbacks()

            lateinit var state2: State
            lateinit var state22: State

            val machine = createTestStateMachine(coroutineStarterType, start = false) {
                callbacks.listen(this)

                initialState("state1") { callbacks.listen(this) }
                state2 = state("state2") {
                    callbacks.listen(this)

                    initialState("state21") { callbacks.listen(this) }
                    state22 = state("state22") { callbacks.listen(this) }
                }

                onStarted { callbacks.onStarted(this) }
            }

            machine.startFromBlocking(state22)

            verifySequenceAndClear(callbacks) {
                callbacks.onStarted(machine)
                callbacks.onStateEntry(machine)
                callbacks.onStateEntry(state2)
                callbacks.onStateEntry(state22)
            }

            machine.stopBlocking()
            machine.activeStates().shouldBeEmpty()

            machine.startFromBlocking("state22")

            verifySequenceAndClear(callbacks) {
                callbacks.onStarted(machine)
                callbacks.onStateEntry(machine)
                callbacks.onStateEntry(state2)
                callbacks.onStateEntry(state22)
            }
        }

        "data startFromBlocking()" {
            val callbacks = mockkCallbacks()
            lateinit var state2: DataState<Int>
            val machine = createTestStateMachine(coroutineStarterType, start = false) {
                initialState("state1") {
                    callbacks.listen(this)
                }
                state2 = dataState("state2", defaultData = 1) {
                    callbacks.listen(this)
                }
            }
            machine.startFromBlocking(state2, data = 42)
            state2.data shouldBe 42
        }

        "data startFrom()" {
            val callbacks = mockkCallbacks()
            lateinit var state2: DataState<Int>
            val machine = createTestStateMachine(coroutineStarterType, start = false) {
                initialState("state1") {
                    callbacks.listen(this)
                }
                state2 = dataState("state2", defaultData = 1) {
                    callbacks.listen(this)
                }
            }
            machine.startFrom(state2, data = 42)
            state2.data shouldBe 42
        }
    }
})