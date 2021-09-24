package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowUnit
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.should
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.Called
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import ru.nsk.kstatemachine.Testing.startFrom

private object OnEvent : Event
private object OffEvent : Event

class StateMachineTest {
    @Test
    fun noInitialState() {
        shouldThrow<IllegalStateException> {
            createStateMachine {}
        }
    }

    @Test
    fun onOffDsl() {
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

    @Test
    fun genericOnTransitionNotification() {
        val callbacks = mockkCallbacks()

        val machine = createStateMachine {
            initialState("first") {
                transition<SwitchEvent>()
            }

            onTransition { _, _, event, _ ->
                callbacks.onTriggeredTransition(event)
            }
        }

        machine.processEvent(SwitchEvent)
        verifySequence { callbacks.onTriggeredTransition(SwitchEvent) }
    }

    @Test
    fun currentStateNotification() {
        val callbacks = mockkCallbacks()
        lateinit var first: State

        val machine = createStateMachine {
            first = initialState("first")
        }
        machine.onStateChanged { callbacks.onStateChanged(it) }

        verifySequence { callbacks.onStateChanged(first) }
    }

    @Test
    fun addSameStateListener() {
        createStateMachine {
            initialState("first") {
                transition<SwitchEvent>()
                val listener = object : IState.Listener {}
                addListener(listener)
                shouldThrow<IllegalArgumentException> { addListener(listener) }
            }
        }
    }

    @Test
    fun addSameStateMachineListener() {
        createStateMachine {
            initialState("first") {
                transition<SwitchEvent>()
            }

            val listener = object : StateMachine.Listener {}
            addListener(listener)
            shouldThrow<IllegalArgumentException> { addListener(listener) }
        }
    }

    @Test
    fun addSameTransitionListener() {
        createStateMachine {
            initialState("first") {
                val transition = transition<SwitchEvent>()
                val listener = object : Transition.Listener {}
                transition.addListener(listener)
                shouldThrow<IllegalArgumentException> { transition.addListener(listener) }
            }
        }
    }

    @Test
    fun addStateAfterStart() {
        val machine = createStateMachine {
            initialState("first")
        }
        shouldThrow<IllegalStateException> { machine.state() }
    }

    @Test
    fun setInitialStateAfterStart() {
        lateinit var first: State
        val machine = createStateMachine {
            first = initialState("first")
        }

        shouldThrowUnit<IllegalStateException> { machine.setInitialState(first) }
    }

    @Test
    fun pendingEventHandler() {
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

    @Test
    fun requireState() {
        lateinit var first: State
        lateinit var second: State
        val machine = createStateMachine {
            first = initialState("first")
            second = state("second")
        }

        machine.requireState("first") shouldBeSameInstanceAs first
        machine.requireState("second", recursive = false) shouldBeSameInstanceAs second
        shouldThrow<IllegalArgumentException> { machine.requireState("third") }
    }

    @Test
    fun requireStateRecursive() {
        lateinit var first: State
        lateinit var firstNested: State
        val machine = createStateMachine {
            first = initialState("first") {
                firstNested = initialState("firstNested")
            }
        }

        machine.requireState("firstNested") shouldBeSameInstanceAs firstNested
        shouldThrow<IllegalArgumentException> {
            machine.requireState("firstNested", recursive = false) shouldBeSameInstanceAs firstNested
        }
        first.requireState("firstNested", recursive = false) shouldBeSameInstanceAs firstNested
    }

    @Test
    fun processEventBeforeStarted() {
        createStateMachine {
            initialState("first")
            shouldThrow<IllegalStateException> { processEvent(SwitchEvent) }
        }
    }

    @Test
    fun onStartedListener() {
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

    @Test
    fun finishingStateMachine() {
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

    @Test
    fun finishedStateMachineIgnoresEvent() {
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

    @Test
    fun stateMachineEntryExit() {
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

    @Test
    fun startFrom() {
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

    @Test
    fun restartMachine() {
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

    @Test
    fun activeStates() {
        lateinit var state1: State
        lateinit var state2: State
        lateinit var state21: State
        lateinit var state211: State

        val machine = createStateMachine {
            state1 = initialState("state1") {
                transitionOn<SwitchEvent> {
                    targetState = { state2 }
                }
            }
            state2 = state("state2") {
                state21 = initialState("state21") {
                    state211 = addInitialState(createStateMachine(start = false) {
                        // should not be included
                        initialState("state2111")
                    })
                }
            }
        }

        var activeStates = machine.activeStates()
        activeStates should containExactly(machine, state1)

        machine.processEvent(SwitchEvent)

        activeStates = machine.activeStates()
        activeStates should containExactly(machine, state2, state21, state211)
    }
}
