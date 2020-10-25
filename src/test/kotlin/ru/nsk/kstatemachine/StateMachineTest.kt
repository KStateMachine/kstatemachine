package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import io.kotest.assertions.throwables.shouldThrow
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
            val first = state("first") {
                transition<SwitchEvent>()
            }
            setInitialState(first)

            onTransition { _, _, event, _ ->
                callbacks.onTriggeringEvent(event)
            }
        }

        stateMachine.processEvent(SwitchEvent)
        then(callbacks).should().onTriggeringEvent(SwitchEvent)
    }

    @Test
    fun addSameStateListener() {
        val stateMachine = createStateMachine {
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
        val stateMachine = createStateMachine {
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
        val stateMachine = createStateMachine {
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
            val first = state("first")
            setInitialState(first)

            ignoredEventHandler = StateMachine.IgnoredEventHandler { _, _, _ ->
                callbacks.onIgnoredEvent(SwitchEvent)
            }
        }

        stateMachine.processEvent(SwitchEvent)
        then(callbacks).should().onIgnoredEvent(SwitchEvent)
    }

    @Test
    fun exceptionalIgnoredEventHandler() {
        val stateMachine = createStateMachine {
            val first = state("first")
            setInitialState(first)

            ignoredEventHandler = StateMachine.IgnoredEventHandler { _, event, _ ->
                error("unexpected $event")
            }
        }

        shouldThrow<IllegalStateException> {
            stateMachine.processEvent(SwitchEvent)
        }
    }
}