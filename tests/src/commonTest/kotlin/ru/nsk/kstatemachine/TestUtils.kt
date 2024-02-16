@file:OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
package ru.nsk.kstatemachine

import io.mockk.MockKVerificationScope
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlin.coroutines.EmptyCoroutineContext

typealias Callback<T> = (T) -> Unit

object SwitchEvent : Event
object SwitchEventL1 : Event
object SwitchEventL2 : Event

object FirstEvent : Event
object SecondEvent : Event

interface Callbacks {
    fun onStarted(machine: StateMachine)
    fun onIgnoredEvent(event: Event)
    fun onTransitionTriggered(event: Event)
    fun onTransitionTriggered(event: Event, index: Int)
    fun onTransitionComplete(event: Event)
    fun onStateEntry(state: IState)
    fun onStateExit(state: IState)
    fun onStateFinished(state: IState)
    fun onStopped(machine: StateMachine)
    fun onDestroyed(machine: StateMachine)
}

fun Callbacks.listen(state: IState) {
    state.onEntry { onStateEntry(this) }
    state.onExit { onStateExit(this) }
    state.onFinished { onStateFinished(this) }
}

inline fun <reified E : Event> Callbacks.listen(transitionBuilder: TransitionBuilder<E>) {
    transitionBuilder.onTriggered { onTransitionTriggered(it.event) }
}

fun mockkCallbacks() = mockk<Callbacks>(relaxUnitFun = true)

fun verifySequenceAndClear(mock: Any, verifyBlock: MockKVerificationScope.() -> Unit) {
    verifySequence(verifyBlock = verifyBlock)
    clearMocks(mock, answers = false)
}

fun testError(message: String = "test exception"): Nothing {
    throw TestException(message)
}

class TestException(message: String) : RuntimeException(message)

enum class CoroutineStarterType {
    STD_LIB,
    COROUTINES_LIB_EMPTY_CONTEXT,
    COROUTINES_LIB_UNCONFINED_DISPATCHER,

    /**
     * Tests touch machines from 2 threads this way (main and coroutine worker),
     * but it should be ok as it happens sequentially.
     */
    COROUTINES_LIB_SINGLE_THREAD_DISPATCHER,
    COROUTINES_LIB_DEFAULT_LIMITED_DISPATCHER,
}

/**
 * Wraps [createStdLibStateMachine] so it can be easily switched to [createStdLibStateMachine]
 */
fun createTestStateMachine(
    coroutineStarterType: CoroutineStarterType,
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    start: Boolean = true,
    autoDestroyOnStatesReuse: Boolean = true,
    enableUndo: Boolean = false,
    doNotThrowOnMultipleTransitionsMatch: Boolean = false,
    init: suspend BuildingStateMachine.() -> Unit
) = when (coroutineStarterType) {
    CoroutineStarterType.STD_LIB -> createStdLibStateMachine(
        name,
        childMode,
        start,
        autoDestroyOnStatesReuse,
        enableUndo,
        doNotThrowOnMultipleTransitionsMatch,
        init = init
    )
    CoroutineStarterType.COROUTINES_LIB_EMPTY_CONTEXT -> createStateMachineBlocking(
        CoroutineScope(EmptyCoroutineContext),
        name,
        childMode,
        start,
        autoDestroyOnStatesReuse,
        enableUndo,
        doNotThrowOnMultipleTransitionsMatch,
        init = init
    )
    CoroutineStarterType.COROUTINES_LIB_UNCONFINED_DISPATCHER -> createStateMachineBlocking(
        CoroutineScope(Dispatchers.Unconfined),
        name,
        childMode,
        start,
        autoDestroyOnStatesReuse,
        enableUndo,
        doNotThrowOnMultipleTransitionsMatch,
        init = init
    )
    CoroutineStarterType.COROUTINES_LIB_SINGLE_THREAD_DISPATCHER -> createStateMachineBlocking(
        CoroutineScope(newSingleThreadContext("")),
        name,
        childMode,
        start,
        autoDestroyOnStatesReuse,
        enableUndo,
        doNotThrowOnMultipleTransitionsMatch,
        init = init
    )
    CoroutineStarterType.COROUTINES_LIB_DEFAULT_LIMITED_DISPATCHER -> createStateMachineBlocking(
        CoroutineScope(Dispatchers.Default.limitedParallelism(1)),
        name,
        childMode,
        start,
        autoDestroyOnStatesReuse,
        enableUndo,
        doNotThrowOnMultipleTransitionsMatch,
        init = init
    )
}