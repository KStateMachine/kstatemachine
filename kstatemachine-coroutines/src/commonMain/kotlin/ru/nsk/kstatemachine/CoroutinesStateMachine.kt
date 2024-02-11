package ru.nsk.kstatemachine

import kotlinx.coroutines.CoroutineScope

/**
 * Suspendable analog of [createStdLibStateMachine] function, with Kotlin Coroutines support.
 * This is preferred function. Use this one especially if you are going to use Kotlin Coroutines library from
 * KStateMachine callbacks.
 *
 * @param scope be careful while working with threaded scopes as KStateMachine classes are not thread-safe.
 * Usually you should use only single threaded scopes, for example:
 *
 *     CoroutineScope(Dispatchers.Default.limitedParallelism(1))
 *
 * Note that all calls to created machine instance should be done only from that thread.
 */
suspend fun createStateMachine(
    scope: CoroutineScope,
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    start: Boolean = true,
    autoDestroyOnStatesReuse: Boolean = true,
    enableUndo: Boolean = false,
    doNotThrowOnMultipleTransitionsMatch: Boolean = false,
    metaInfo: StateMetaInfo? = null,
    init: suspend BuildingStateMachine.() -> Unit
) = CoroutinesLibCoroutineAbstraction(scope).createStateMachine(
    name,
    childMode,
    start,
    autoDestroyOnStatesReuse,
    enableUndo,
    doNotThrowOnMultipleTransitionsMatch,
    metaInfo,
    init
)

fun createStateMachineBlocking(
    scope: CoroutineScope,
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    start: Boolean = true,
    autoDestroyOnStatesReuse: Boolean = true,
    enableUndo: Boolean = false,
    doNotThrowOnMultipleTransitionsMatch: Boolean = false,
    metaInfo: StateMetaInfo? = null,
    init: suspend BuildingStateMachine.() -> Unit
) = with(CoroutinesLibCoroutineAbstraction(scope)) {
    runBlocking {
        createStateMachine(
            name,
            childMode,
            start,
            autoDestroyOnStatesReuse,
            enableUndo,
            doNotThrowOnMultipleTransitionsMatch,
            metaInfo,
            init
        )
    }
}