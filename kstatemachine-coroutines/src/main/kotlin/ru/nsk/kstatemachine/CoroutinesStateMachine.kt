package ru.nsk.kstatemachine

import kotlinx.coroutines.CoroutineScope

/**
 * Analog of [createStdLibStateMachine] function, with Kotlin Coroutines support.
 * This is preferred function. Use this one especially if you are going to use Kotlin Coroutines library from
 * KStateMachine callbacks.
 *
 * @param scope be careful while working with threaded scopes as KStateMachine classes are not thread-safe.
 * Usually you should use only single threaded scopes, for example:
 * 
 *     CoroutineScope(Dispatchers.Default.limitedParallelism(1))
 *
 * Note that all calls to this machine instance should be done only from that thread.
 */
fun createStateMachineBlocking(
    scope: CoroutineScope,
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    start: Boolean = true,
    autoDestroyOnStatesReuse: Boolean = true,
    enableUndo: Boolean = false,
    doNotThrowOnMultipleTransitionsMatch: Boolean = false,
    init: BuildingStateMachine.() -> Unit
) = with(CoroutinesLibCoroutineAbstraction(scope)) {
    runBlocking {
        createStateMachine(
            name,
            childMode,
            start,
            autoDestroyOnStatesReuse,
            enableUndo,
            doNotThrowOnMultipleTransitionsMatch,
            init
        )
    }
}

suspend fun createStateMachine(
    scope: CoroutineScope,
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    start: Boolean = true,
    autoDestroyOnStatesReuse: Boolean = true,
    enableUndo: Boolean = false,
    doNotThrowOnMultipleTransitionsMatch: Boolean = false,
    init: BuildingStateMachine.() -> Unit
) = CoroutinesLibCoroutineAbstraction(scope).createStateMachine(
    name,
    childMode,
    start,
    autoDestroyOnStatesReuse,
    enableUndo,
    doNotThrowOnMultipleTransitionsMatch,
    init
)