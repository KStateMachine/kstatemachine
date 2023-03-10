package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import kotlin.coroutines.EmptyCoroutineContext

class CoroutinesTest : StringSpec({
    /** Coroutines manipulations like withContext or launch from coroutineScope make test fail. */
    "call suspend functions from major listeners and callbacks" {
        val machine = createStdLibStateMachine {
            onStarted {
                delay(0)
            }
            onStopped { delay(0) }
            onTransition { delay(0) }
            onTransitionComplete { _, _ -> delay(0) }
            onStateEntry { delay(0) }
            val first = initialState("first") {
                onEntry { delay(0) }
                onExit { delay(0) }
                onFinished { delay(0) }

                val transition = transition<SwitchEvent> {
                    guard = {
                        delay(0)
                        true
                    }
                    onTriggered { delay(0) }
                }
                transition.onTriggered { delay(0) }

                transitionConditionally<SecondEvent> {
                    direction = {
                        delay(0)
                        stay()
                    }
                }
            }
            choiceState {
                delay(0)
                first
            }
        }
        machine.processEventBlocking(SwitchEvent)
    }

    "using coroutines with std lib throws" {
        shouldThrow<UnsupportedOperationException> {
            createStdLibStateMachine {
                initialState()
                onStarted { delay(100) }
            }
        }
    }

    "test coroutines called from machine callbacks" {
        val scope = CoroutineScope(EmptyCoroutineContext)
        try {
            createStateMachine(scope) {
                onStarted { delay(1) }
                initialState("first") {
                    onEntry {
                        coroutineScope {
                            scope.launch { delay(1) }
                            scope.launch { delay(1) }
                        }
                        withContext(Dispatchers.Default) {
                            delay(1)
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
        val scope = CoroutineScope(newSingleThreadContext("test thread"))
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
        }
    }

    "current thread context preserving by suspend methods called from threads" {
        runBlocking {
            val thread = Thread.currentThread()
            withContext(Dispatchers.IO) {
                createStateMachine(this@runBlocking) {
                    onStarted { Thread.currentThread()  shouldBe thread }
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
                    if (machine.isRunning) { /* do something */ }
                    check(Thread.currentThread() == machineThread)
                }
            }
        }
    }
})