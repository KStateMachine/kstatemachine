/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

@file:OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)

package ru.nsk.kstatemachine

import io.mockk.MockKVerificationScope
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.*
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.*
import ru.nsk.kstatemachine.transition.TransitionBuilder
import ru.nsk.kstatemachine.transition.onTriggered
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

private val singleThreadContext = newSingleThreadContext("test single thread context") // fixme context leaks
/**
 * Wraps [createStdLibStateMachine] so it can be easily switched to [createStdLibStateMachine]
 */
fun createTestStateMachine(
    coroutineStarterType: CoroutineStarterType,
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    start: Boolean = true,
    creationArguments: CreationArguments = buildCreationArguments {},
    init: suspend BuildingStateMachine.() -> Unit
) = when (coroutineStarterType) {
    CoroutineStarterType.STD_LIB -> createStdLibStateMachine(
        name,
        childMode,
        start,
        creationArguments,
        init = init
    )
    CoroutineStarterType.COROUTINES_LIB_EMPTY_CONTEXT -> createStateMachineBlocking(
        CoroutineScope(EmptyCoroutineContext), // does not perform internal context switching
        name,
        childMode,
        start,
        creationArguments,
        init = init
    )
    CoroutineStarterType.COROUTINES_LIB_UNCONFINED_DISPATCHER -> createStateMachineBlocking(
        CoroutineScope(Dispatchers.Unconfined),
        name,
        childMode,
        start,
        creationArguments,
        init = init
    )
    CoroutineStarterType.COROUTINES_LIB_SINGLE_THREAD_DISPATCHER -> createStateMachineBlocking(
        CoroutineScope(singleThreadContext),
        name,
        childMode,
        start,
        creationArguments,
        init = init
    )
    CoroutineStarterType.COROUTINES_LIB_DEFAULT_LIMITED_DISPATCHER -> createStateMachineBlocking(
        CoroutineScope(Dispatchers.Default.limitedParallelism(1)), // does not guarantee same thread for each task
        name,
        childMode,
        start,
        creationArguments,
        init = init
    )
}