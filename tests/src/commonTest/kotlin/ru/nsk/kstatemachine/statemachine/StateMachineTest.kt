/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.statemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowUnit
import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.called
import io.mockk.verify
import io.mockk.verifySequence
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.event.EventMatcher
import ru.nsk.kstatemachine.event.StartEvent
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachineTestData.OffEvent
import ru.nsk.kstatemachine.statemachine.StateMachineTestData.OnEvent
import ru.nsk.kstatemachine.testing.Testing.startFromBlocking
import ru.nsk.kstatemachine.transition.DefaultTransition
import ru.nsk.kstatemachine.transition.Transition
import ru.nsk.kstatemachine.transition.TransitionType
import ru.nsk.kstatemachine.transition.onTriggered

private object StateMachineTestData {
    object OnEvent : Event
    object OffEvent : Event
}

class StateMachineTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "no initial state" {
                shouldThrow<IllegalStateException> {
                    createTestStateMachine(coroutineStarterType) {}
                }
            }

            "on off dsl sample" {
                val callbacks = mockkCallbacks()

                lateinit var on: State
                lateinit var off: State

                val machine = createTestStateMachine(coroutineStarterType) {
                    on = initialState("on") {
                        callbacks.listen(this)
                    }
                    off = state("off") {
                        callbacks.listen(this)

                        transition<OnEvent> {
                            targetState = on
                            callbacks.listen(this)
                        }
                    }

                    on {
                        transition<OffEvent> {
                            targetState = off
                            callbacks.listen(this)
                        }
                    }
                }

                verifySequenceAndClear(callbacks) { callbacks.onStateEntry(on) }

                machine.processEventBlocking(OffEvent)
                verifySequenceAndClear(callbacks) {
                    callbacks.onTransitionTriggered(OffEvent)
                    callbacks.onStateExit(on)
                    callbacks.onStateEntry(off)
                }

                machine.processEventBlocking(OnEvent)
                verifySequenceAndClear(callbacks) {
                    callbacks.onTransitionTriggered(OnEvent)
                    callbacks.onStateExit(off)
                    callbacks.onStateEntry(on)
                }

                machine.processEventBlocking(OnEvent)
                verify { callbacks wasNot called }
            }

            "non dsl usage" {
                val machine =
                    createTestStateMachine(coroutineStarterType, name = "machine", start = false) { /* empty */ }
                val first = DefaultState("first")
                val second = DefaultState("second")
                val third = DefaultState("third")
                second.addInitialState(third)

                second.onEntry { println("$name entered") }

                val transition = DefaultTransition<SwitchEvent>(
                    "transition", EventMatcher.isInstanceOf(), TransitionType.LOCAL, null, first, second
                )
                transition.onTriggered { println("${it.transition.name} triggered") }

                first.addTransition(transition)

                machine.addInitialState(first)
                machine.addState(second)
                machine.startBlocking()

                machine.processEventBlocking(SwitchEvent)

                second.isActive shouldBe true
            }

            "onTransitionTriggered() notification" {
                val callbacks = mockkCallbacks()

                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState("first") {
                        transition<SwitchEvent>()
                    }

                    onTransitionTriggered { callbacks.onTransitionTriggered(it.event) }
                }

                machine.processEventBlocking(SwitchEvent)
                verifySequence {
                    callbacks.onTransitionTriggered(ofType<StartEvent>())
                    callbacks.onTransitionTriggered(SwitchEvent)
                }
            }

            "onTransitionComplete() notification" {
                val callbacks = mockkCallbacks()

                lateinit var state1: State
                lateinit var state2: State
                lateinit var state22: State
                val machine = createTestStateMachine(coroutineStarterType) {
                    state1 = initialState("state1") {
                        transitionOn<SwitchEvent> { targetState = { state2 } }
                    }

                    state2 = state("state2") {
                        state22 = initialState("state22")
                    }

                    onTransitionComplete { activeStates, transitionParams ->
                        when (transitionParams.event) {
                            is StartEvent -> activeStates.shouldContainExactlyInAnyOrder(state1)
                            SwitchEvent -> {
                                callbacks.onTransitionTriggered(transitionParams.event)
                                activeStates.shouldContainExactlyInAnyOrder(state2, state22)
                            }
                        }
                    }
                }

                machine.processEventBlocking(SwitchEvent)
                verifySequence { callbacks.onTransitionTriggered(SwitchEvent) }
            }


            "onEntryState() notification" {
                val callbacks = mockkCallbacks()
                lateinit var first: State

                val machine = createTestStateMachine(coroutineStarterType) {
                    first = initialState("first")
                    onStateEntry { state, _ -> callbacks.onStateEntry(state) }
                }

                verifySequence {
                    callbacks.onStateEntry(machine)
                    callbacks.onStateEntry(first)
                }
            }

            "add same state listener" {
                createTestStateMachine(coroutineStarterType) {
                    initialState("first") {
                        transition<SwitchEvent>()
                        val listener = object : IState.Listener {}
                        addListener(listener)
                        shouldThrow<IllegalArgumentException> { addListener(listener) }
                        removeListener(listener)
                    }
                }
            }

            "add same state machine listener" {
                createTestStateMachine(coroutineStarterType) {
                    initialState("first") {
                        transition<SwitchEvent>()
                    }

                    val listener = object : StateMachine.Listener {}
                    addListener(listener)
                    shouldThrow<IllegalArgumentException> { addListener(listener) }
                    removeListener(listener)
                }
            }

            "add same transition listener" {
                createTestStateMachine(coroutineStarterType) {
                    initialState("first") {
                        val transition = transition<SwitchEvent>()
                        val listener = object : Transition.Listener {}
                        transition.addListener(listener)
                        shouldThrow<IllegalArgumentException> { transition.addListener(listener) }
                        transition.removeListener(listener)
                    }
                }
            }

            "add state after start" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState("first")
                }
                shouldThrow<IllegalStateException> { machine.state() }
            }

            "set initial state after start" {
                lateinit var first: State
                val machine = createTestStateMachine(coroutineStarterType) {
                    first = initialState("first")
                }

                shouldThrowUnit<IllegalStateException> { machine.setInitialState(first) }
            }

            "process event before started" {
                val machine = createTestStateMachine(coroutineStarterType, start = false) {
                    initialState("first")
                }
                shouldThrow<IllegalStateException> {
                    machine.processEventBlocking(SwitchEvent)
                }
            }

            "onStarted() listener" {
                val callbacks = mockkCallbacks()

                lateinit var first: State
                val machine = createTestStateMachine(coroutineStarterType) {
                    first = initialState { callbacks.listen(this) }
                    onStarted { callbacks.onStarted(this) }
                }

                verifySequence {
                    callbacks.onStarted(machine)
                    callbacks.onStateEntry(first)
                }
            }

            "state machine entry exit" {
                val callbacks = mockkCallbacks()

                lateinit var initialState: State

                val machine = createTestStateMachine(coroutineStarterType) {
                    callbacks.listen(this)

                    initialState = initialState("initial") {
                        callbacks.listen(this)
                    }
                }

                verifySequence {
                    callbacks.onStateEntry(machine)
                    callbacks.onStateEntry(initialState)
                }
            }

            "restart machine after stop" {
                val callbacks = mockkCallbacks()

                lateinit var state1: State
                lateinit var state2: State

                val machine = createTestStateMachine(coroutineStarterType, start = false) {
                    logger = StateMachine.Logger { println(it()) }
                    callbacks.listen(this)

                    state1 = initialState("state1") { callbacks.listen(this) }
                    state2 = state("state2") { callbacks.listen(this) }

                    onStarted { callbacks.onStarted(this) }
                    onStopped { callbacks.onStopped(this) }
                }

                machine.startFromBlocking(state2)

                verifySequenceAndClear(callbacks) {
                    callbacks.onStarted(machine)
                    callbacks.onStateEntry(machine)
                    callbacks.onStateEntry(state2)
                }

                machine.stopBlocking()
                machine.stopBlocking() // does nothing
                verifySequenceAndClear(callbacks) { callbacks.onStopped(machine) }

                machine.startBlocking()
                verifySequence {
                    callbacks.onStarted(machine)
                    callbacks.onStateEntry(machine)
                    callbacks.onStateEntry(state1)
                }
            }

            "state machine listener callbacks sequence" {
                val callbacks = mockkCallbacks()
                lateinit var state1: State
                lateinit var state2: State

                val machine = createTestStateMachine(coroutineStarterType) {
                    logger = StateMachine.Logger { println(it()) }

                    state1 = initialState("state1") {
                        transitionOn<SwitchEvent> { targetState = { state2 } }
                    }
                    state2 = finalState("state2")

                    onStarted { callbacks.onStarted(this) }
                    onStateEntry { state, _ -> callbacks.onStateEntry(state) }
                    onFinished { callbacks.onStateFinished(this) }
                }

                machine.processEventBlocking(SwitchEvent)

                verifySequence {
                    callbacks.onStarted(machine)
                    callbacks.onStateEntry(machine)
                    callbacks.onStateEntry(state1)
                    callbacks.onStateEntry(state2)
                    callbacks.onStateFinished(machine)
                }
            }

            "stop from onStart" {
                createTestStateMachine(coroutineStarterType) {
                    initialState("initial")
                    onStarted { stop() }
                }
            }

            "destroy from onStart" {
                val callbacks = mockkCallbacks()
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState("initial")
                    onStarted {
                        callbacks.onStarted(this)
                        destroy()
                    }
                    onStopped { callbacks.onStopped(this) }
                    onDestroyed { callbacks.onDestroyed(this) }
                }
                machine.isRunning shouldBe false
                machine.isDestroyed shouldBe true

                verifySequence {
                    callbacks.onStarted(machine)
                    callbacks.onStopped(machine)
                    callbacks.onDestroyed(machine)
                }
            }

            table(
                headers("stop"),
                row(false),
                row(true),
            ).forAll { stop ->
                "destroy stopped machine" {
                    val callbacks = mockkCallbacks()
                    val machine = createTestStateMachine(coroutineStarterType) {
                        initialState("initial")
                        onStopped { callbacks.onStopped(this) }
                    }
                    machine.stop()
                    machine.destroy(stop)

                    machine.isDestroyed shouldBe true
                    verifySequence { callbacks.onStopped(machine) }
                }
            }

            "hasProcessedEvents flag indicates processing of the first (except StartEvent) event" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState()
                    transition<SwitchEvent>()

                    machine.hasProcessedEvents shouldBe false
                    onStarted {
                        machine.hasProcessedEvents shouldBe false
                    }
                }
                machine.hasProcessedEvents shouldBe false
                machine.processEvent(FirstEvent) shouldBe ProcessingResult.IGNORED
                machine.hasProcessedEvents shouldBe false
                machine.processEvent(SwitchEvent) shouldBe ProcessingResult.PROCESSED
                machine.hasProcessedEvents shouldBe true
                machine.stop()
                machine.hasProcessedEvents shouldBe true
                machine.start()
                machine.hasProcessedEvents shouldBe false
                machine.processEvent(SwitchEvent)
                machine.hasProcessedEvents shouldBe true
            }

            "processEventByAsync()" {
                val machine = createStateMachine(this) {
                    initialState()
                    transition<SwitchEvent>()
                }
                val deferred = machine.processEventByAsync(SwitchEvent)
                deferred.await() shouldBe ProcessingResult.PROCESSED
            }

            "processEventByLaunch()" {
                val machine = createStateMachine(this) {
                    initialState()
                    transition<SwitchEvent>()
                }
                machine.processEventByLaunch(SwitchEvent)
            }

            "negative processEventByAsync() throws" {
                val machine = createStdLibStateMachine {
                    initialState()
                    transition<SwitchEvent>()
                }
                shouldThrow<IllegalArgumentException> {
                    machine.processEventByAsync(SwitchEvent)
                }
            }

            "negative processEventByLaunch() throws" {
                val machine = createStdLibStateMachine {
                    initialState()
                    transition<SwitchEvent>()
                }
                shouldThrow<IllegalArgumentException> {
                    machine.processEventByLaunch(SwitchEvent)
                }
            }
        }
    }
})
