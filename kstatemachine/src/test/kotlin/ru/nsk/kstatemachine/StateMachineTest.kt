package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowUnit
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.sameInstance
import org.junit.jupiter.api.Test

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
        lateinit var first: State

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

        lateinit var first: State
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

        lateinit var final: State
        val machine = createStateMachine {
            final = finalState("final") {
                onEntry { callbacks.onEntryState(this) }
                onExit { callbacks.onExitState(this) }
            }
            setInitialState(final)

            onFinished { callbacks.onFinished(this) }
        }

        then(callbacks).should().onEntryState(final)
        then(callbacks).should().onFinished(machine)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun stateMachineEntryExit() {
        val callbacks = mock<Callbacks>()

        lateinit var initialState: State

        val machine = createStateMachine {
            callbacks.listen(this)

            initialState = initialState("initial") {
                callbacks.listen(this)
            }
        }

        then(callbacks).should().onEntryState(machine)
        then(callbacks).should().onEntryState(initialState)
    }
}