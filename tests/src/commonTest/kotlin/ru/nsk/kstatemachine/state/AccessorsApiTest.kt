/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.transition.Transition

private open class BaseTestEvent : Event
private class DerivedTestEvent : BaseTestEvent()

class AccessorsApiTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "requireTransition()" {
                val state = object : DefaultState() {}

                lateinit var firstTransition: Transition<*>
                lateinit var secondTransition: Transition<*>

                createTestStateMachine(coroutineStarterType) {
                    addInitialState(state) {
                        firstTransition = transition<FirstEvent>("first transition")
                        secondTransition = transition<SecondEvent>()
                    }
                }

                state.requireTransition("first transition") shouldBeSameInstanceAs firstTransition
                state.requireTransition<SecondEvent>() shouldBeSameInstanceAs secondTransition
                shouldThrowWithMessage<IllegalArgumentException>(
                    "Transition some transition not found"
                ) { state.requireTransition("some transition") }
                shouldThrowWithMessage<IllegalArgumentException>(
                    "Transition for SwitchEvent not found"
                ) { state.requireTransition<SwitchEvent>() }
            }

            "requireState()" {
                lateinit var first: State
                lateinit var second: State
                val machine = createTestStateMachine(coroutineStarterType) {
                    first = initialState("first")
                    second = state("second")
                }

                machine.requireState("first") shouldBeSameInstanceAs first
                machine.requireState("second", recursive = false) shouldBeSameInstanceAs second
                shouldThrowWithMessage<IllegalArgumentException>(
                    "State third not found"
                ) { machine.requireState("third") }
            }

            "requireState() recursive" {
                lateinit var first: State
                lateinit var firstNested: State
                val machine = createTestStateMachine(coroutineStarterType) {
                    first = initialState("first") {
                        firstNested = initialState("firstNested")
                    }
                }

                machine.requireState("firstNested") shouldBeSameInstanceAs firstNested
                shouldThrowWithMessage<IllegalArgumentException>(
                    "State firstNested not found"
                ) { machine.requireState("firstNested", recursive = false) shouldBeSameInstanceAs firstNested }
                first.requireState("firstNested", recursive = false) shouldBeSameInstanceAs firstNested
            }

            "requireState() by type" {
                open class SubclassState : DefaultState()
                open class UnusedSubclassState : DefaultState()

                class FirstState(val value: Int = 42) : DefaultState()
                class SecondState : SubclassState()
                class ThirdInnerState : SubclassState()

                lateinit var first: FirstState
                lateinit var second: State
                lateinit var third: State
                val machine = createTestStateMachine(coroutineStarterType) {
                    first = addInitialState(FirstState())
                    second = addState(SecondState()) {
                        third = addState(ThirdInnerState())
                    }
                }

                machine.requireState<FirstState>() shouldBeSameInstanceAs first
                machine.requireState<FirstState>().value shouldBe first.value
                machine.requireState<SecondState>(recursive = false) shouldBeSameInstanceAs second

                shouldThrowWithMessage<IllegalArgumentException>(
                    "State ThirdInnerState not found"
                ) { machine.requireState<ThirdInnerState>(recursive = false) }
                machine.requireState<ThirdInnerState>() shouldBeSameInstanceAs third

                shouldThrowWithMessage<IllegalArgumentException>(
                    "State UnusedSubclassState not found"
                ) { machine.requireState<UnusedSubclassState>() }
                shouldThrowWithMessage<IllegalArgumentException>(
                    "More than one state matches State"
                ) { machine.requireState<State>() }
                shouldThrowWithMessage<IllegalArgumentException>(
                    "More than one state matches SubclassState"
                ) { machine.requireState<SubclassState>() }
            }

            "requireState() of nested machine not allowed" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState {
                        addInitialState(createTestStateMachine(coroutineStarterType) {
                            initialState("nested")
                            transition<SwitchEvent>("transition")
                        })
                    }
                }

                shouldThrowWithMessage<IllegalArgumentException>(
                    "State nested not found"
                ) { machine.requireState("nested") }
            }

            "findTransition() by event type strict (default)" {
                val state = object : DefaultState() {}

                lateinit var transitionFirst: Transition<*>
                lateinit var transitionSecond: Transition<*>

                createTestStateMachine(coroutineStarterType) {
                    addInitialState(state) {
                        transitionFirst = transition<FirstEvent>("first")
                        transitionSecond = transition<SecondEvent>("second")
                    }
                }

                state.findTransition<FirstEvent>() shouldBeSameInstanceAs transitionFirst
                state.findTransition<SecondEvent>() shouldBeSameInstanceAs transitionSecond
                state.findTransition<SwitchEvent>().shouldBeNull()
            }

            "findTransition(event) finds transition by eventMatcher.match()" {
                val state = object : DefaultState() {}
                lateinit var transitionSwitch: Transition<*>
                lateinit var transitionFirst: Transition<*>

                createTestStateMachine(coroutineStarterType) {
                    addInitialState(state) {
                        transitionSwitch = transition<SwitchEvent>("switch")
                        transitionFirst = transition<FirstEvent>("first")
                    }
                }

                state.findTransition(SwitchEvent) shouldBeSameInstanceAs transitionSwitch
                state.findTransition(FirstEvent) shouldBeSameInstanceAs transitionFirst
                // unrelated event → null
                state.findTransition(SecondEvent).shouldBeNull()
            }

            "findTransition(event) uses isInstanceOf semantics — matches subtype events" {
                // A transition registered for BaseTestEvent with the default isInstanceOf() matcher
                // must match DerivedTestEvent instances because DerivedTestEvent IS-A BaseTestEvent.
                val state = object : DefaultState() {}
                lateinit var transitionBase: Transition<*>

                createTestStateMachine(coroutineStarterType) {
                    addInitialState(state) {
                        transitionBase = transition<BaseTestEvent>("base")
                    }
                }

                state.findTransition(DerivedTestEvent()) shouldBeSameInstanceAs transitionBase
                state.findTransition(SwitchEvent).shouldBeNull()
            }

            "requireTransition(event) throws when no match" {
                val state = object : DefaultState() {}
                createTestStateMachine(coroutineStarterType) {
                    addInitialState(state) {
                        transition<SwitchEvent>()
                    }
                }

                shouldThrow<IllegalArgumentException> {
                    state.requireTransition(FirstEvent)
                }
            }
        }
    }
})