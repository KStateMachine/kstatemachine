package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import io.kotest.assertions.throwables.shouldThrow
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test

private object OnEvent : Event
private object OffEvent : Event

class StateMachineTest {
    @Test
    fun noInitialState() {
        shouldThrow<Exception> {
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
            on = state("on") {
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
            setInitialState(on)

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
    fun addSameStateListener() {
        createStateMachine {
            val first = state("first") {
                transition<SwitchEvent>()
                val listener = object : State.Listener {}
                addListener(listener)
                shouldThrow<IllegalArgumentException> { addListener(listener) }
            }
            setInitialState(first)
        }
    }

    @Test
    fun addSameStateMachineListener() {
        createStateMachine {
            val first = state("first") {
                transition<SwitchEvent>()
            }
            setInitialState(first)

            val listener = object : StateMachine.Listener {}
            addListener(listener)
            shouldThrow<IllegalArgumentException> { addListener(listener) }
        }
    }

    @Test
    fun addSameTransitionListener() {
        createStateMachine {
            val first = state("first") {
                val transition = transition<SwitchEvent>()
                val listener = object : Transition.Listener {}
                transition.addListener(listener)
                shouldThrow<IllegalArgumentException> { transition.addListener(listener) }
            }
            setInitialState(first)
        }
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
}