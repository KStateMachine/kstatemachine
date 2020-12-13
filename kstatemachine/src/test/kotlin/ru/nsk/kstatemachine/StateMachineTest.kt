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

        val stateMachine = createStateMachine {
            on = initialState("on") {
                onEntry { callbacks.onEntryState(this) }
                onExit { callbacks.onExitState(this) }
            }
            off = state("off") {
                onEntry { callbacks.onEntryState(this) }
                onExit { callbacks.onExitState(this) }
                transition<OnEvent> {
                    targetState = on
                    onTriggered {
                        callbacks.onTriggeringEvent(it.event)
                    }
                }
            }

            on {
                transition<OffEvent> {
                    targetState = off
                    onTriggered {
                        callbacks.onTriggeringEvent(it.event)
                    }
                }
            }
        }

        then(callbacks).should(inOrder).onEntryState(on)

        stateMachine.processEvent(OffEvent)
        then(callbacks).should(inOrder).onTriggeringEvent(OffEvent)
        then(callbacks).should(inOrder).onExitState(on)
        then(callbacks).should(inOrder).onEntryState(off)

        stateMachine.processEvent(OnEvent)
        then(callbacks).should(inOrder).onTriggeringEvent(OnEvent)
        then(callbacks).should(inOrder).onExitState(off)
        then(callbacks).should(inOrder).onEntryState(on)

        stateMachine.processEvent(OnEvent)
        then(callbacks).shouldHaveNoMoreInteractions()
    }

    @Test
    fun genericOnTransitionNotification() {
        val callbacks = mock<Callbacks>()

        val stateMachine = createStateMachine {
            initialState("first") {
                transition<SwitchEvent>()
            }

            onTransition { _, _, event, _ ->
                callbacks.onTriggeringEvent(event)
            }
        }

        stateMachine.processEvent(SwitchEvent)
        then(callbacks).should().onTriggeringEvent(SwitchEvent)
    }

    @Test
    fun currentStateNotification() {
        val callbacks = mock<Callbacks>()
        lateinit var first: State

        val stateMachine = createStateMachine {
            first = initialState("first")
        }
        stateMachine.onStateChanged { callbacks.onStateChanged(it) }

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
        val stateMachine = createStateMachine {
            initialState("first")
        }
        shouldThrow<IllegalStateException> { stateMachine.state() }
    }

    @Test
    fun setInitialStateAfterStart() {
        lateinit var first: State
        val stateMachine = createStateMachine {
            first = initialState("first")
        }

        shouldThrowUnit<IllegalStateException> { stateMachine.setInitialState(first) }
    }

    @Test
    fun ignoredEventHandler() {
        val callbacks = mock<Callbacks>()

        val stateMachine = createStateMachine {
            initialState("first")

            ignoredEventHandler = StateMachine.IgnoredEventHandler { _, _, _ ->
                callbacks.onIgnoredEvent(SwitchEvent)
            }
        }

        stateMachine.processEvent(SwitchEvent)
        then(callbacks).should().onIgnoredEvent(SwitchEvent)
    }

    @Test
    fun pendingEventHandler() {
        val stateMachine = createStateMachine {
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

        stateMachine.processEvent(SwitchEvent)
    }

    @Test
    fun exceptionalIgnoredEventHandler() {
        val stateMachine = createStateMachine {
            initialState("first")

            ignoredEventHandler = StateMachine.IgnoredEventHandler { _, event, _ ->
                error("unexpected $event")
            }
        }

        shouldThrow<IllegalStateException> {
            stateMachine.processEvent(SwitchEvent)
        }
    }

    @Test
    fun requireState() {
        lateinit var first: State
        lateinit var second: State
        val stateMachine = createStateMachine {
            first = initialState("first")
            second = state("second")
        }

        assertThat(stateMachine.requireState("first"), sameInstance(first))
        assertThat(stateMachine.requireState("second"), sameInstance(second))
        shouldThrow<IllegalArgumentException> { stateMachine.requireState("third") }
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
        createStateMachine {
            first = initialState {
                onEntry { callbacks.onEntryState(this) }
            }
            onStarted { callbacks.onStarted() }
        }
        then(callbacks).should(inOrder).onStarted()
        then(callbacks).should(inOrder).onEntryState(first)
    }

    @Test
    fun finishingStateMachine() {
        val callbacks = mock<Callbacks>()

        lateinit var final: State
        createStateMachine {
            final = finalState("final") {
                onEntry { callbacks.onEntryState(this) }
                onExit { callbacks.onExitState(this) }
            }
            setInitialState(final)

            onFinished { callbacks.onFinished() }
        }

        then(callbacks).should().onEntryState(final)
        then(callbacks).should().onExitState(final)
        then(callbacks).should().onFinished()
    }

    @Test
    fun processEventOnFinishedStateMachine() {
        val callbacks = mock<Callbacks>()

        lateinit var final: State
        val machine = createStateMachine {
            final = finalState("final") {
            }
            setInitialState(final)

            onFinished { callbacks.onFinished() }

            ignoredEventHandler = StateMachine.IgnoredEventHandler { _, event, _ ->
                callbacks.onIgnoredEvent(event)
            }
        }

        then(callbacks).should().onFinished()

        machine.processEvent(SwitchEvent)
        then(callbacks).shouldHaveNoMoreInteractions()
    }
}