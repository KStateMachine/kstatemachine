package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.verify
import io.mockk.verifySequence
import ru.nsk.kstatemachine.TypesafeTransitionTestData.CustomDataEvent
import ru.nsk.kstatemachine.TypesafeTransitionTestData.IdEvent
import ru.nsk.kstatemachine.TypesafeTransitionTestData.NameEvent

private object TypesafeTransitionTestData {
    class CustomDataEvent(val value: Int) : Event
    class NameEvent(override val data: String) : DataEvent<String>
    class IdEvent(override val data: Int) : DataEvent<Int>
}

class TypesafeTransitionTest : StringSpec({
    CoroutineStarterType.values().forEach { coroutineStarterType ->
        "initial DataState negative" {
            shouldThrow<Exception> {
                createTestStateMachine(coroutineStarterType) {
                    addInitialState(defaultDataState<String>("state1"))
                }
            }
        }

        "initial DataState with defaultData" {
            lateinit var state: DataState<String>
            createTestStateMachine(coroutineStarterType) {
                state = initialDataState("state1", defaultData = "test")
            }
            state.data shouldBe "test"
        }

        "FinalDataState transition" {
            lateinit var final: DataState<Int>

            val machine = createTestStateMachine(coroutineStarterType) {
                initialState("initial") {
                    dataTransitionOn<IdEvent, Int> { targetState = { final } }
                }
                final = finalDataState("final")
            }

            machine.processEvent(IdEvent(42))
            machine.activeStates().shouldContainExactly(final)
        }

        "single data state" {
            val testName = "testName"

            lateinit var state2: DataState<String>

            val machine = createTestStateMachine(coroutineStarterType) {
                val state3 = state("state3")

                state2 = dataState("state2") {
                    onEntry { data shouldBe testName }
                    onExit { shouldThrow<IllegalStateException> { data } }
                    transition<SwitchEvent> { targetState = state3 }
                }

                initialState("state1") {
                    dataTransition<NameEvent, String> { targetState = state2 }
                }
            }

            shouldThrow<IllegalStateException> { state2.data }

            machine.processEvent(NameEvent(testName))
            state2.data shouldBe testName

            machine.processEvent(SwitchEvent)
            shouldThrow<IllegalStateException> { state2.data }
        }

        "multiple data states" {
            lateinit var state2: DataState<String>
            lateinit var state3: DataState<Int>

            val machine = createTestStateMachine(coroutineStarterType) {
                initialState("state1") {
                    dataTransitionOn<NameEvent, String> { targetState = { state2 } }
                }
                state2 = dataState("state2") {
                    dataTransitionOn<IdEvent, Int> { targetState = { state3 } }
                }
                state3 = dataState("state3")
            }

            val name = "testName"
            machine.processEvent(NameEvent(name))

            state2.data shouldBe name

            val id = 42
            machine.processEvent(IdEvent(id))

            shouldThrow<IllegalStateException> { state2.data }
            state3.data shouldBe id
        }

        "multiple nested data states" {
            val callbacks = mockkCallbacks()
            lateinit var state1: State
            lateinit var state2: DataState<String>
            lateinit var state21: State
            lateinit var state22: DataState<Int>

            val machine = createTestStateMachine(coroutineStarterType) {
                state1 = initialState("state1") {
                    callbacks.listen(this)

                    dataTransitionOn<NameEvent, String> { targetState = { state2 } }
                }
                state2 = dataState("state2") {
                    callbacks.listen(this)

                    state21 = initialState("state21") {
                        callbacks.listen(this)

                        dataTransitionOn<IdEvent, Int> { targetState = { state22 } }
                    }
                    state22 = dataState("state22") { callbacks.listen(this) }
                }
            }

            verifySequenceAndClear(callbacks) { callbacks.onEntryState(state1) }

            val name = "testName"
            machine.processEvent(NameEvent(name))
            verifySequenceAndClear(callbacks) {
                callbacks.onExitState(state1)
                callbacks.onEntryState(state2)
                callbacks.onEntryState(state21)
            }

            state2.data shouldBe name

            val id = 42
            machine.processEvent(IdEvent(id))
            verifySequence {
                callbacks.onExitState(state21)
                callbacks.onEntryState(state22)
            }

            state2.data shouldBe name
            state22.data shouldBe id
        }

        "implicit data state activation by cross-level transition negative" {
            val machine = createTestStateMachine(coroutineStarterType) {
                lateinit var state21: State

                initialState {
                    transitionOn<SwitchEvent> { targetState = { state21 } }
                }
                dataState<Int> {
                    onEntry { println(data) }

                    state21 = initialState()
                }
            }

            shouldThrow<IllegalStateException> { machine.processEvent(SwitchEvent) }
        }

        "implicit data state activation by cross-level transition with default value" {
            val machine = createTestStateMachine(coroutineStarterType) {
                lateinit var state21: State

                initialState {
                    transitionOn<SwitchEvent> { targetState = { state21 } }
                }
                dataState(defaultData = 1) {
                    onEntry { println(data) }

                    state21 = initialState()
                }
            }

            machine.processEvent(SwitchEvent)
        }

        "transition with event super type" {
            lateinit var state2: DataState<Number>

            val machine = createTestStateMachine(coroutineStarterType) {
                state2 = dataState("state2")

                initialState("state1") {
                    dataTransition<IdEvent, Number> { targetState = state2 }
                }
            }

            val id = 42
            machine.processEvent(IdEvent(id))

            state2.data shouldBe id
        }

        "target-less data transition negative" {
            shouldThrow<IllegalArgumentException> {
                createTestStateMachine(coroutineStarterType) {
                    initialState("state1") {
                        dataTransition<IdEvent, Int> {}
                    }
                }
            }
        }

        "target-less transition in data state" {
            val callbacks = mockkCallbacks()

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it) }

                val dataState = dataState<Int>("state2") {
                    transition<SwitchEvent> { callbacks.listen(this) }
                }

                initialState("state1") {
                    dataTransition<IdEvent, Int>(targetState = dataState)
                }
            }

            machine.processEvent(IdEvent(13))
            machine.processEvent(SwitchEvent)

            verify { callbacks.onTriggeredTransition(SwitchEvent) }
        }

        "self targeted transition in data state" {
            shouldThrow<IllegalArgumentException> {
                createTestStateMachine(coroutineStarterType) {
                    initialState("state1")

                    dataState("state2") {
                        dataTransition<IdEvent, Int>(targetState = this)
                    }
                }
            }
        }

        "self targeted transitionOn() does not update data, cannot throw on construction" {
            lateinit var dataState: DataState<Int>

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it) }

                initialState("state1") {
                    dataTransitionOn<IdEvent, Int> { targetState = { dataState } }
                }

                dataState = dataState("state2") {
                    dataTransitionOn<IdEvent, Int> { targetState = { dataState } }
                }
            }

            machine.processEvent(IdEvent(1))
            dataState.data shouldBe 1

            machine.processEvent(IdEvent(2))
            dataState.data shouldBe 1
        }

        "targeting DataState by conditionalTransition()" {
            lateinit var dataState: DataState<Int>
            createTestStateMachine(coroutineStarterType) {
                initialState {
                    setInitialState(finalDataState(defaultData = 0))

                    transitionConditionally<FinishedEvent> {
                        direction = { targetState(dataState) }
                    }
                }
                dataState = dataState(defaultData = 42)
            }
            dataState.data shouldBe 0
            dataState.lastData shouldBe 0
        }

        "targeting DataState by conditionalTransition() with custom extractor" {
            lateinit var dataState: DataState<Int>
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState {
                    transitionConditionally<CustomDataEvent> {
                        direction = { targetState(dataState) }
                    }
                }
                dataState = dataState(
                    "data state",
                    dataExtractor = object : DataExtractor<Int> {
                        override suspend fun extractFinishedEvent(transitionParams: TransitionParams<*>, event: FinishedEvent) =
                            event.data as? Int

                        override suspend fun extract(transitionParams: TransitionParams<*>): Int? {
                            return (transitionParams.event as? CustomDataEvent)?.value
                        }
                    }
                )
            }
            machine.processEvent(CustomDataEvent(42))
            dataState.data shouldBe 42
            dataState.lastData shouldBe 42
        }
    }
})