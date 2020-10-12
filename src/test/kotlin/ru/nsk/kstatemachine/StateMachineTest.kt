package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

object SomeEvent : Event

object OnEvent : Event
object OffEvent : Event

interface Callbacks {
    fun onTriggeringEvent(event: Event)
    fun onEntryState(state: State)
    fun onExitState(state: State)
}

class StateMachineTest {
    @Test
    fun noInitialState() {
        shouldThrow<Exception> {
            val stateMachine = createStateMachine {
                state("test")
            }
            stateMachine.processEvent(OnEvent)
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
    fun conditionalTransition() {
        val callbacks = mock<Callbacks>()

        val stateMachine = createStateMachine {
            val first = state("first") {
                transitionConditionally<SomeEvent> {
                    direction = { stay() }
                    onTriggered {
                        callbacks.onTriggeringEvent(it.event)
                    }
                }
            }
            setInitialState(first)
        }

        stateMachine.processEvent(SomeEvent)
        then(callbacks).should().onTriggeringEvent(SomeEvent)
    }

    @Test
    fun genericOnTransitionNotification() {
        val callbacks = mock<Callbacks>()

        val stateMachine = createStateMachine {
            val first = state("first") {
                transition<SomeEvent>()
            }
            setInitialState(first)

            onTransition {_, _, event, _ ->
                callbacks.onTriggeringEvent(event)
            }
        }

        stateMachine.processEvent(SomeEvent)
        then(callbacks).should().onTriggeringEvent(SomeEvent)
    }
}