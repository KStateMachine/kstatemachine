package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import com.nhaarman.mockitokotlin2.times
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowUnit
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.should
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.sameInstance
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
        val callbacks = mock<Callbacks>()
        val inOrder = inOrder(callbacks)

        lateinit var on: UnitState
        lateinit var off: UnitState

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

        then(callbacks).should(inOrder).onEntryState(on)

        machine.processEvent(OffEvent)
        then(callbacks).should(inOrder).onTriggeredTransition(OffEvent)
        then(callbacks).should(inOrder).onExitState(on)
        then(callbacks).should(inOrder).onEntryState(off)

        machine.processEvent(OnEvent)
        then(callbacks).should(inOrder).onTriggeredTransition(OnEvent)
        then(callbacks).should(inOrder).onExitState(off)
        then(callbacks).should(inOrder).onEntryState(on)

        machine.processEvent(OnEvent)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun genericOnTransitionNotification() {
        val callbacks = mock<Callbacks>()

        val machine = createStateMachine {
            initialState("first") {
                transition<SwitchEvent>()
            }

            onTransition { _, _, event, _ ->
                callbacks.onTriggeredTransition(event)
            }
        }

        machine.processEvent(SwitchEvent)
        then(callbacks).should().onTriggeredTransition(SwitchEvent)
    }

    @Test
    fun currentStateNotification() {
        val callbacks = mock<Callbacks>()
        lateinit var first: UnitState

        val machine = createStateMachine {
            first = initialState("first")
        }
        machine.onStateChanged { callbacks.onStateChanged(it) }

        then(callbacks).should().onStateChanged(first)
    }

    @Test
    fun addSameStateListener() {
        createStateMachine {
            initialState("first") {
                transition<SwitchEvent>()
                val listener = object : State.Listener {}
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
        lateinit var first: UnitState
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
        lateinit var first: UnitState
        lateinit var second: UnitState
        val machine = createStateMachine {
            first = initialState("first")
            second = state("second")
        }

        assertThat(machine.requireState("first"), sameInstance(first))
        assertThat(machine.requireState("second"), sameInstance(second))
        shouldThrow<IllegalArgumentException> { machine.requireState("third") }
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
        val callbacks = mock<Callbacks>()
        val inOrder = inOrder(callbacks)

        lateinit var first: UnitState
        val machine = createStateMachine {
            first = initialState {
                onEntry { callbacks.onEntryState(this) }
            }
            onStarted { callbacks.onStarted(this) }
        }
        then(callbacks).should(inOrder).onStarted(machine)
        then(callbacks).should(inOrder).onEntryState(first)
    }

    @Test
    fun finishingStateMachine() {
        val callbacks = mock<Callbacks>()

        lateinit var final: UnitState
        val machine = createStateMachine {
            final = finalState("final") { callbacks.listen(this) }
            setInitialState(final)

            onFinished { callbacks.onFinished(this) }
        }

        then(callbacks).should().onEntryState(final)
        then(callbacks).should().onFinished(machine)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun finishedStateMachineIgnoresEvent() {
        val callbacks = mock<Callbacks>()

        lateinit var final: UnitState
        val machine = createStateMachine {
            final = finalState("final") { callbacks.listen(this) }
            setInitialState(final)

            onFinished { callbacks.onFinished(this) }

            transition<SwitchEvent> {
                targetState = final
                callbacks.listen(this)
            }
        }

        then(callbacks).should().onEntryState(final)
        then(callbacks).should().onFinished(machine)
        then(callbacks).shouldHaveNoMoreInteractions()

        machine.processEvent(SwitchEvent)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun stateMachineEntryExit() {
        val callbacks = mock<Callbacks>()

        lateinit var initialState: UnitState

        val machine = createStateMachine {
            callbacks.listen(this)

            initialState = initialState("initial") {
                callbacks.listen(this)
            }
        }

        then(callbacks).should().onEntryState(machine)
        then(callbacks).should().onEntryState(initialState)
    }

    @Test
    fun startFrom() {
        val callbacks = mock<Callbacks>()

        lateinit var state2: UnitState
        lateinit var state22: UnitState

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

        then(callbacks).should().onStarted(machine)
        then(callbacks).should().onEntryState(machine)
        then(callbacks).should().onEntryState(state2)
        then(callbacks).should().onEntryState(state22)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun restartMachine() {
        val callbacks = mock<Callbacks>()

        lateinit var state1: UnitState
        lateinit var state2: UnitState

        val machine = createStateMachine(start = false) {
            logger = StateMachine.Logger { println(it) }
            callbacks.listen(this)

            state1 = initialState("state1") { callbacks.listen(this) }
            state2 = state("state2") { callbacks.listen(this) }

            onStarted { callbacks.onStarted(this) }
            onStopped { callbacks.onStopped(this) }
        }

        machine.startFrom(state2)

        then(callbacks).should().onStarted(machine)
        then(callbacks).should().onStarted(machine)
        then(callbacks).should().onEntryState(machine)
        then(callbacks).should().onEntryState(state2)

        machine.stop()
        then(callbacks).should().onStopped(machine)
        then(callbacks).shouldHaveNoMoreInteractions()

        machine.start()
        then(callbacks).should(times(2)).onStarted(machine)
        then(callbacks).should(times(2)).onEntryState(machine)
        then(callbacks).should().onEntryState(state1)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun activeStates() {
        lateinit var state1: UnitState
        lateinit var state2: UnitState
        lateinit var state21: UnitState
        lateinit var state211: UnitState

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
