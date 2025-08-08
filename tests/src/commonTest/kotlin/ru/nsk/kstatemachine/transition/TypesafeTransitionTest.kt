/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.transition

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.verify
import io.mockk.verifySequence
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.event.DataEvent
import ru.nsk.kstatemachine.event.DataExtractor
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.event.FinishedEvent
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.processEventBlocking
import ru.nsk.kstatemachine.transition.TypesafeTransitionTestData.CustomDataEvent
import ru.nsk.kstatemachine.transition.TypesafeTransitionTestData.IdEvent
import ru.nsk.kstatemachine.transition.TypesafeTransitionTestData.NameEvent

private object TypesafeTransitionTestData {
    class CustomDataEvent(val value: Int) : Event
    class NameEvent(override val data: String) : DataEvent<String>
    class IdEvent(override val data: Int) : DataEvent<Int>
}

class TypesafeTransitionTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
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

            machine.processEventBlocking(IdEvent(42))
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

            machine.processEventBlocking(NameEvent(testName))
            state2.data shouldBe testName

            machine.processEventBlocking(SwitchEvent)
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
            machine.processEventBlocking(NameEvent(name))

            state2.data shouldBe name

            val id = 42
            machine.processEventBlocking(IdEvent(id))

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

            verifySequenceAndClear(callbacks) { callbacks.onStateEntry(state1) }

            val name = "testName"
            machine.processEventBlocking(NameEvent(name))
            verifySequenceAndClear(callbacks) {
                callbacks.onStateExit(state1)
                callbacks.onStateEntry(state2)
                callbacks.onStateEntry(state21)
            }

            state2.data shouldBe name

            val id = 42
            machine.processEventBlocking(IdEvent(id))
            verifySequence {
                callbacks.onStateExit(state21)
                callbacks.onStateEntry(state22)
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

            shouldThrow<IllegalStateException> { machine.processEventBlocking(SwitchEvent) }
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

            machine.processEventBlocking(SwitchEvent)
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
            machine.processEventBlocking(IdEvent(id))

            state2.data shouldBe id
        }

        "target-less data transition inside nonDataState negative" {
            shouldThrow<IllegalArgumentException> {
                createTestStateMachine(coroutineStarterType) {
                    initialState("state1") {
                        dataTransition<IdEvent, Int> {}
                    }
                }
            }
        }

        "create self targeted data transition in DataState" {
            createTestStateMachine(coroutineStarterType) {
                initialDataState<Int>("state1", defaultData = 42) {
                    dataTransition<IdEvent, Int>(targetState = this)
                }
            }
        }

        "create self targeted data transition in DataState via builder" {
            createTestStateMachine(coroutineStarterType) {
                initialDataState<Int>("state1", defaultData = 42) {
                    dataTransition<IdEvent, Int> {
                        targetState = this@initialDataState
                    }
                }
            }
        }

        "create target-less data transition in DataState" {
            createTestStateMachine(coroutineStarterType) {
                initialDataState<Int>("state1", defaultData = 42) {
                    // this method is only available for DataState
                    dataTransition<IdEvent, Int>()
                }
            }
        }

        "simple target-less transition in data state" {
            val callbacks = mockkCallbacks()

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it()) }

                val dataState = dataState<Int>("state2") {
                    transition<SwitchEvent> { callbacks.listen(this) }
                }

                initialState("state1") {
                    dataTransition<IdEvent, Int>(targetState = dataState)
                }
            }

            machine.processEventBlocking(IdEvent(13))
            machine.processEventBlocking(SwitchEvent)

            verify { callbacks.onTransitionTriggered(SwitchEvent) }
        }

        "self targeted LOCAL transition in data state, does not update data value" {
            lateinit var dataState: DataState<Int>
            val machine = createTestStateMachine(coroutineStarterType) {
                dataState = initialDataState("state1", defaultData = 1) {
                    dataTransition<IdEvent, Int>(targetState = this)
                }
            }

            dataState.data shouldBe 1
            machine.processEvent(IdEvent(2))
            dataState.data shouldBe 1
        }

        "self targeted EXTERNAL transition in data state updates data value" {
            lateinit var dataState: DataState<Int>
            val machine = createTestStateMachine(coroutineStarterType) {
                dataState = initialDataState("state1", defaultData = 1) {
                    dataTransition<IdEvent, Int>(targetState = this, type = TransitionType.EXTERNAL)
                }
            }

            dataState.data shouldBe 1
            machine.processEvent(IdEvent(2))
            dataState.data shouldBe 2
        }

        "self targeted LOCAL transitionOn() does not update data" {
            lateinit var dataState: DataState<Int>

            val machine = createTestStateMachine(coroutineStarterType) {
                logger = StateMachine.Logger { println(it()) }

                initialState("state1") {
                    dataTransitionOn<IdEvent, Int> { targetState = { dataState } }
                }

                dataState = dataState("state2") {
                    dataTransitionOn<IdEvent, Int> { targetState = { dataState } }
                }
            }

            machine.processEventBlocking(IdEvent(1))
            dataState.data shouldBe 1

            machine.processEventBlocking(IdEvent(2))
            dataState.data shouldBe 1
        }

        "targeting DataState by conditionalTransition()" {
            lateinit var dataState: DataState<Int>
            createTestStateMachine(coroutineStarterType) {
                initialState {
                    initialFinalDataState(defaultData = 0)

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
                        override val dataClass = Int::class

                        override suspend fun extractFinishedEvent(
                            transitionParams: TransitionParams<*>,
                            event: FinishedEvent
                        ) = event.data as? Int

                        override suspend fun extract(
                            transitionParams: TransitionParams<*>,
                            isImplicitActivation: Boolean
                        ): Int? {
                            return (transitionParams.event as? CustomDataEvent)?.value
                        }
                    }
                )
            }
            machine.processEventBlocking(CustomDataEvent(42))
            dataState.data shouldBe 42
            dataState.lastData shouldBe 42
        }
    }
})