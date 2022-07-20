package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowUnit
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.verify
import io.mockk.verifySequence
import ru.nsk.kstatemachine.Testing.startFrom

private object OnEvent : Event
private object OffEvent : Event

class StateMachineTest : StringSpec({
    "no initial state" {
        shouldThrow<IllegalStateException> {
            createStateMachine {}
        }
    }

    "on off dsl sample" {
        val callbacks = mockkCallbacks()

        lateinit var on: State
        lateinit var off: State

        val machine = createStateMachine {
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

        verifySequenceAndClear(callbacks) { callbacks.onEntryState(on) }

        machine.processEvent(OffEvent)
        verifySequenceAndClear(callbacks) {
            callbacks.onTriggeredTransition(OffEvent)
            callbacks.onExitState(on)
            callbacks.onEntryState(off)
        }

        machine.processEvent(OnEvent)
        verifySequenceAndClear(callbacks) {
            callbacks.onTriggeredTransition(OnEvent)
            callbacks.onExitState(off)
            callbacks.onEntryState(on)
        }

        machine.processEvent(OnEvent)
        verify { callbacks wasNot Called }
    }

    "non dsl usage" {
        val machine = StateMachineImpl("machine", ChildMode.EXCLUSIVE, true)
        val first = DefaultState("first")
        val second = DefaultState("second")

        second.onEntry { println("$name entered") }

        val transition = DefaultTransition<SwitchEvent>("transition", EventMatcher.isInstanceOf(), first, second)
        transition.onTriggered { println("${it.transition.name} triggered") }

        first.addTransition(transition)

        machine.addInitialState(first)
        machine.addState(second)
        machine.start()

        machine.processEvent(SwitchEvent)

        second.isActive shouldBe true
    }

    "onTransition() notification" {
        val callbacks = mockkCallbacks()

        val machine = createStateMachine {
            initialState("first") {
                transition<SwitchEvent>()
            }

            onTransition { callbacks.onTriggeredTransition(it.event) }
        }

        machine.processEvent(SwitchEvent)
        verifySequence { callbacks.onTriggeredTransition(SwitchEvent) }
    }

    "onStateChanged() notification" {
        val callbacks = mockkCallbacks()
        lateinit var first: State

        createStateMachine {
            first = initialState("first")
            onStateChanged { callbacks.onStateChanged(it) }
        }

        verifySequence { callbacks.onStateChanged(first) }
    }

    "add same state listener" {
        createStateMachine {
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
        createStateMachine {
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
        createStateMachine {
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
        val machine = createStateMachine {
            initialState("first")
        }
        shouldThrow<IllegalStateException> { machine.state() }
    }

    "set initial state after start" {
        lateinit var first: State
        val machine = createStateMachine {
            first = initialState("first")
        }

        shouldThrowUnit<IllegalStateException> { machine.setInitialState(first) }
    }

    "pending event handler" {
        val machine = createStateMachine {
            val second = state("second")
            initialState("first") {
                transition<SwitchEvent> {
                    targetState = second
                    onTriggered {
                        shouldThrow<IllegalStateException> { this@createStateMachine.processEvent(SwitchEvent) }
                    }
                }
            }

            pendingEventHandler = StateMachine.PendingEventHandler { _, _ ->
                error("Already processing")
            }
        }

        machine.processEvent(SwitchEvent)
    }

    "process event before started" {
        createStateMachine {
            initialState("first")
            shouldThrow<IllegalStateException> { processEvent(SwitchEvent) }
        }
    }

    "onStarted() listener" {
        val callbacks = mockkCallbacks()

        lateinit var first: State
        val machine = createStateMachine {
            first = initialState {
                onEntry { callbacks.onEntryState(this) }
            }
            onStarted { callbacks.onStarted(this) }
        }

        verifySequence {
            callbacks.onStarted(machine)
            callbacks.onEntryState(first)
        }
    }

    "finishing state machine" {
        val callbacks = mockkCallbacks()

        lateinit var final: State
        val machine = createStateMachine {
            final = finalState("final") { callbacks.listen(this) }
            setInitialState(final)

            onFinished { callbacks.onFinished(this) }
        }

        verifySequence {
            callbacks.onEntryState(final)
            callbacks.onFinished(machine)
        }
    }

    "finished state machine ignores event" {
        val callbacks = mockkCallbacks()

        lateinit var final: State
        val machine = createStateMachine {
            final = finalState("final") { callbacks.listen(this) }
            setInitialState(final)

            onFinished { callbacks.onFinished(this) }

            transition<SwitchEvent> {
                targetState = final
                callbacks.listen(this)
            }
        }

        verifySequenceAndClear(callbacks) {
            callbacks.onEntryState(final)
            callbacks.onFinished(machine)
        }

        machine.processEvent(SwitchEvent)
        verify { callbacks wasNot Called }
    }

    "state machine entry exit" {
        val callbacks = mockkCallbacks()

        lateinit var initialState: State

        val machine = createStateMachine {
            callbacks.listen(this)

            initialState = initialState("initial") {
                callbacks.listen(this)
            }
        }

        verifySequence {
            callbacks.onEntryState(machine)
            callbacks.onEntryState(initialState)
        }
    }

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

    "restart machine" {
        val callbacks = mockkCallbacks()

        lateinit var state1: State
        lateinit var state2: State

        val machine = createStateMachine(start = false) {
            logger = StateMachine.Logger { println(it) }
            callbacks.listen(this)

            state1 = initialState("state1") { callbacks.listen(this) }
            state2 = state("state2") { callbacks.listen(this) }

            onStarted { callbacks.onStarted(this) }
            onStopped { callbacks.onStopped(this) }
        }

        machine.startFrom(state2)

        verifySequenceAndClear(callbacks) {
            callbacks.onStarted(machine)
            callbacks.onEntryState(machine)
            callbacks.onEntryState(state2)
        }

        machine.stop()
        verifySequenceAndClear(callbacks) { callbacks.onStopped(machine) }

        machine.start()
        verifySequence {
            callbacks.onStarted(machine)
            callbacks.onEntryState(machine)
            callbacks.onEntryState(state1)
        }
    }

    "state machine listener callbacks sequence" {
        val callbacks = mockkCallbacks()
        lateinit var state1: State
        lateinit var state2: State

        val machine = createStateMachine {
            logger = StateMachine.Logger { println(it) }

            state1 = initialState("state1") {
                transitionOn<SwitchEvent> { targetState = { state2 } }
            }
            state2 = finalState("state2")

            onStarted { callbacks.onStarted(this) }
            onStateChanged { callbacks.onStateChanged(it) }
            onFinished { callbacks.onFinished(this) }
        }

        machine.processEvent(SwitchEvent)

        verifySequence {
            callbacks.onStarted(machine)
            callbacks.onStateChanged(state1)
            callbacks.onStateChanged(state2)
            callbacks.onFinished(machine)
        }
    }
})
