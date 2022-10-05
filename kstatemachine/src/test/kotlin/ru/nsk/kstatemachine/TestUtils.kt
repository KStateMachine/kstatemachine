package ru.nsk.kstatemachine

import io.mockk.MockKVerificationScope
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verifySequence

typealias Callback<T> = (T) -> Unit

object SwitchEvent : Event
object SwitchEventL1 : Event
object SwitchEventL2 : Event

object FirstEvent : Event
object SecondEvent : Event

interface Callbacks {
    fun onStarted(machine: StateMachine)
    fun onStopped(machine: StateMachine)
    fun onFinished(state: IState)
    fun onIgnoredEvent(event: Event)
    fun onTriggeredTransition(event: Event)
    fun onTriggeredTransition(event: Event, index: Int)
    fun onEntryState(state: IState)
    fun onExitState(state: IState)
}

fun Callbacks.listen(state: IState) {
    state.onEntry { onEntryState(this) }
    state.onExit { onExitState(this) }
    state.onFinished { onFinished(this) }
}

inline fun <reified E : Event> Callbacks.listen(transitionBuilder: TransitionBuilder<E>) {
    transitionBuilder.onTriggered { onTriggeredTransition(it.event) }
}

fun mockkCallbacks() = mockk<Callbacks>(relaxUnitFun = true)

fun verifySequenceAndClear(mock: Any, verifyBlock: MockKVerificationScope.() -> Unit) {
    verifySequence(verifyBlock = verifyBlock)
    clearMocks(mock)
}

fun testError(message: String): Nothing {
    throw TestException(message)
}

class TestException(message: String) : RuntimeException(message)