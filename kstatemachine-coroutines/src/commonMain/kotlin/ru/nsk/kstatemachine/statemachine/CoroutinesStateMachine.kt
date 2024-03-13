package ru.nsk.kstatemachine.statemachine

import kotlinx.coroutines.CoroutineScope
import ru.nsk.kstatemachine.coroutines.CoroutinesLibCoroutineAbstraction
import ru.nsk.kstatemachine.coroutines.createStateMachine
import ru.nsk.kstatemachine.state.ChildMode

/**
 * Suspendable analog of [createStdLibStateMachine] function, with Kotlin Coroutines support.
 * This is preferred function. Use this one especially if you are going to use Kotlin Coroutines library from
 * KStateMachine callbacks.
 *
 * @param scope be careful while working with threaded scopes as KStateMachine classes are not thread-safe.
 * Usually you should use only single threaded scopes, for example:
 *
 *     CoroutineScope(newSingleThreadContext("single threaded context"))
 *
 * Note that all calls to created machine instance should be done only from that thread.
 */
suspend fun createStateMachine(
    scope: CoroutineScope,
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    start: Boolean = true,
    creationArguments: StateMachine.CreationArguments = StateMachine.CreationArguments(),
    init: suspend BuildingStateMachine.() -> Unit
) = CoroutinesLibCoroutineAbstraction(scope)
    .createStateMachine(name, childMode, start, creationArguments, init)