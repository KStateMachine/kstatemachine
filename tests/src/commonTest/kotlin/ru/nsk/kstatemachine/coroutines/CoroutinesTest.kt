/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.coroutines

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.verifySequence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ru.nsk.kstatemachine.SecondEvent
import ru.nsk.kstatemachine.SwitchEvent
import ru.nsk.kstatemachine.event.StartEvent
import ru.nsk.kstatemachine.mockkCallbacks
import ru.nsk.kstatemachine.state.State
import ru.nsk.kstatemachine.state.asyncScopedAction
import ru.nsk.kstatemachine.state.choiceState
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.onEntry
import ru.nsk.kstatemachine.state.onExit
import ru.nsk.kstatemachine.state.onFinished
import ru.nsk.kstatemachine.state.state
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.state.transitionConditionally
import ru.nsk.kstatemachine.state.transitionOn
import ru.nsk.kstatemachine.statemachine.StateMachineNotification.Destroyed
import ru.nsk.kstatemachine.statemachine.StateMachineNotification.Started
import ru.nsk.kstatemachine.statemachine.StateMachineNotification.StateEntry
import ru.nsk.kstatemachine.statemachine.StateMachineNotification.StateExit
import ru.nsk.kstatemachine.statemachine.StateMachineNotification.StateFinished
import ru.nsk.kstatemachine.statemachine.StateMachineNotification.Stopped
import ru.nsk.kstatemachine.statemachine.StateMachineNotification.TransitionComplete
import ru.nsk.kstatemachine.statemachine.StateMachineNotification.TransitionTriggered
import ru.nsk.kstatemachine.statemachine.activeStatesFlow
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.statemachine.createStdLibStateMachine
import ru.nsk.kstatemachine.statemachine.destroy
import ru.nsk.kstatemachine.statemachine.onDestroyed
import ru.nsk.kstatemachine.statemachine.onStarted
import ru.nsk.kstatemachine.statemachine.onStateEntry
import ru.nsk.kstatemachine.statemachine.onStateExit
import ru.nsk.kstatemachine.statemachine.onStateFinished
import ru.nsk.kstatemachine.statemachine.onStopped
import ru.nsk.kstatemachine.statemachine.onTransitionComplete
import ru.nsk.kstatemachine.statemachine.onTransitionTriggered
import ru.nsk.kstatemachine.statemachine.processEventBlocking
import ru.nsk.kstatemachine.statemachine.stateMachineNotificationFlow
import ru.nsk.kstatemachine.transition.onTriggered
import ru.nsk.kstatemachine.transition.stay
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class CoroutinesTest : FreeSpec({
    /** Coroutines manipulations like withContext or launch from coroutineScope make test fail. */
    "call suspend functions from major listeners and callbacks" {
        val machine = createStdLibStateMachine {
            onStarted {
                delay(0.milliseconds)
            }
            onTransitionTriggered { delay(0.milliseconds) }
            onTransitionComplete { _, _ -> delay(0.milliseconds) }
            onStateEntry { _, _ -> delay(0.milliseconds) }
            onStateExit { _, _ -> delay(0.milliseconds) }
            onStateFinished { _, _ -> delay(0.milliseconds) }
            onStopped { delay(0.milliseconds) }
            onDestroyed { delay(0.milliseconds) }
            val first = initialState("first") {
                onEntry { delay(0.milliseconds) }
                onExit { delay(0.milliseconds) }
                onFinished { delay(0.milliseconds) }

                val transition = transition<SwitchEvent> {
                    guard = {
                        delay(0.milliseconds)
                        true
                    }
                    onTriggered { delay(0.milliseconds) }
                }
                transition.onTriggered { delay(0.milliseconds) }

                transitionConditionally<SecondEvent> {
                    direction = {
                        delay(0.milliseconds)
                        stay()
                    }
                }
            }
            choiceState {
                delay(0.milliseconds)
                first
            }
        }
        machine.processEventBlocking(SwitchEvent)
    }

    "using coroutines with std lib throws" {
        shouldThrowWithMessage<IllegalStateException>(
            "Seems that you are trying to use Kotlin Coroutines library from KStateMachine callbacks, use kstatemachine-coroutines support library to make that work"
        ) {
            createStdLibStateMachine {
                initialState()
                onStarted { delay(100.milliseconds) }
            }
        }
    }

    "test coroutines called from machine callbacks" {
        val scope = CoroutineScope(EmptyCoroutineContext)
        try {
            createStateMachine(scope) {
                onStarted { delay(1.milliseconds) }
                initialState("first") {
                    onEntry {
                        coroutineScope {
                            scope.launch { delay(1.milliseconds) }
                            scope.launch { delay(1.milliseconds) }
                        }
                        withContext(Dispatchers.Default) {
                            delay(1.milliseconds)
                        }
                    }
                }
            }
        } finally {
            scope.cancel()
        }
    }

    "test context preserving by suspend methods called from threads" test@{
        val thread = Thread.currentThread()
        withContext(Dispatchers.IO) {
            println("${Thread.currentThread()}")
            createStateMachine(this@test) {
                onStarted { Thread.currentThread() shouldBe thread }
                initialState()
            }
        }
    }

    "empty context does not preserve machine if suspend methods called from threads" {
        val scope = CoroutineScope(EmptyCoroutineContext)
        withContext(Dispatchers.IO) {
            val thread = Thread.currentThread()
            createStateMachine(scope) {
                onStarted { Thread.currentThread() shouldBe thread }
                initialState()
            }
        }
    }

    "threaded context preserving by suspend methods called from threads" {
        val context = newSingleThreadContext("test thread")
        val scope = CoroutineScope(context)
        try {
            val thread = runBlocking(scope.coroutineContext) { Thread.currentThread() }
            println(thread)

            withContext(Dispatchers.IO) {
                println("io" + Thread.currentThread())
                createStateMachine(scope) {
                    onStarted { Thread.currentThread() shouldBe thread }
                    initialState()
                }
            }
        } finally {
            scope.cancel()
            context.close()
        }
    }

    "current thread context preserving by suspend methods called from threads" {
        runBlocking {
            val thread = Thread.currentThread()
            withContext(Dispatchers.IO) {
                createStateMachine(this@runBlocking) {
                    onStarted { Thread.currentThread() shouldBe thread }
                    initialState()
                }
            }
        }
    }

    "threading sample for docs" {
        runBlocking { // defines non empty coroutine context for state machine
            val machineThread = Thread.currentThread()
            val machineScope = this

            val machine = createStateMachine(machineScope) {
                onStarted { check(Thread.currentThread() == machineThread) }

                val state2 = state("state2")
                initialState("state1") {
                    transition<SwitchEvent> {
                        targetState = state2
                        onTriggered { check(Thread.currentThread() == machineThread) }
                    }
                }
            }

            withContext(Dispatchers.Default) {
                check(Thread.currentThread() != machineThread) // suppose we are working from some other thread

                // OK, will be processed on state machine context as `processEvent` is suspendable and switches context
                // internally and context is not EmptyCoroutineContext
                machine.processEvent(SwitchEvent)

                // But this is NOT OK, this will be a race condition as this property is muted from state machines thread
                // if (machine.isRunning) { /* do something */ }

                withContext(machineScope.coroutineContext) {
                    // OK again as we switched context explicitly before accessing property
                    if (machine.isRunning) {
                        Unit// do something
                    }
                    check(Thread.currentThread() == machineThread)
                }
            }
        }
    }

    "machine notification flow with replay" {
        val state1: State
        lateinit var state2: State
        val machine = createStateMachine(this, start = false) {
            state1 = initialState("state1") {
                transitionOn<SwitchEvent> { targetState = { state2 } }
            }
            state2 = state("state2")
        }

        val eventsCount = 11
        val notificationFlow = machine.stateMachineNotificationFlow(replay = eventsCount)

        machine.start()
        machine.processEvent(SwitchEvent)
        machine.destroy()

        val callbacks = mockkCallbacks()
        notificationFlow.take(eventsCount).collect {
            when (it) {
                is Started -> callbacks.onStarted(it.machine)
                is TransitionTriggered -> callbacks.onTransitionTriggered(it.transitionParams.event)
                is TransitionComplete -> callbacks.onTransitionComplete(it.transitionParams.event)
                is StateEntry -> callbacks.onStateEntry(it.state)
                is StateExit -> callbacks.onStateExit(it.state)
                is StateFinished -> callbacks.onStateFinished(it.state)
                is Stopped -> callbacks.onStopped(it.machine)
                is Destroyed -> callbacks.onDestroyed(it.machine)
            }
        }

        verifySequence {
            callbacks.onStarted(machine)
            callbacks.onStateEntry(machine)
            callbacks.onTransitionTriggered(ofType<StartEvent>())
            callbacks.onStateEntry(state1)
            callbacks.onTransitionComplete(ofType<StartEvent>())
            callbacks.onTransitionTriggered(SwitchEvent)
            callbacks.onStateExit(state1)
            callbacks.onStateEntry(state2)
            callbacks.onTransitionComplete(SwitchEvent)
            callbacks.onStopped(machine)
            callbacks.onDestroyed(machine)
        }
    }

    "machine notification flow" {
        val state1: State
        lateinit var state2: State
        val machine = createStateMachine(this, start = false) {
            state1 = initialState("state1") {
                transitionOn<SwitchEvent> { targetState = { state2 } }
            }
            state2 = state("state2")
        }

        val eventsCount = 11
        val callbacks = mockkCallbacks()

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            machine.stateMachineNotificationFlow()
                .take(eventsCount)
                .collect {
                    println("$it")
                    when (it) {
                        is Started -> callbacks.onStarted(it.machine)
                        is TransitionTriggered -> callbacks.onTransitionTriggered(it.transitionParams.event)
                        is TransitionComplete -> callbacks.onTransitionComplete(it.transitionParams.event)
                        is StateEntry -> callbacks.onStateEntry(it.state)
                        is StateExit -> callbacks.onStateExit(it.state)
                        is StateFinished -> callbacks.onStateFinished(it.state)
                        is Stopped -> callbacks.onStopped(it.machine)
                        is Destroyed -> callbacks.onDestroyed(it.machine)
                    }
                }
        }

        machine.start()
        machine.processEvent(SwitchEvent)
        machine.destroy()
        job.join()

        verifySequence {
            callbacks.onStarted(machine)
            callbacks.onStateEntry(machine)
            callbacks.onTransitionTriggered(ofType<StartEvent>())
            callbacks.onStateEntry(state1)
            callbacks.onTransitionComplete(ofType<StartEvent>())
            callbacks.onTransitionTriggered(SwitchEvent)
            callbacks.onStateExit(state1)
            callbacks.onStateEntry(state2)
            callbacks.onTransitionComplete(SwitchEvent)
            callbacks.onStopped(machine)
            callbacks.onDestroyed(machine)
        }
    }

    "activeStatesFlow()" {
        val state1: State
        lateinit var state2: State
        val machine = createStateMachine(this) {
            state1 = initialState("state1") {
                transitionOn<SwitchEvent> { targetState = { state2 } }
            }
            state2 = state("state2")
        }
        val statesFlow = machine.activeStatesFlow()
        statesFlow.first().shouldContainExactlyInAnyOrder(state1)

        machine.processEvent(SwitchEvent)
        statesFlow.first().shouldContainExactlyInAnyOrder(state2)
    }

    "context switching" {
        println("" + Thread.currentThread() + Thread.currentThread().hashCode())
        val scope = CoroutineScope(EmptyCoroutineContext)
        val scope1 = CoroutineScope(Dispatchers.Default)
        val scope2 = CoroutineScope(Dispatchers.IO)
        scope.launch {
            println(
                "EmptyCoroutineContext ${this.coroutineContext} "
                        + Thread.currentThread()
                        + Thread.currentThread().hashCode()
            )
        }
        scope1.launch {
            println(
                "Default               ${this.coroutineContext} "
                        + Thread.currentThread()
                        + Thread.currentThread().hashCode()
            )
        }
        scope2.launch {
            println(
                "IO                    ${this.coroutineContext} "
                        + Thread.currentThread()
                        + Thread.currentThread().hashCode()
            )
        }
    }
})