package ru.nsk.kstatemachine

import kotlinx.coroutines.CoroutineScope

/**
 * Analog of [createStateMachine] function, with Kotlin Coroutines support.
 * Use this one if you are going to use Kotlin Coroutines library from KStateMachine callbacks.
 *
 * @param scope be careful while working with threaded scopes as KStateMachine classes are not thread-safe.
 * Usually you should use only single threaded scopes, for example:
 * 
 *     CoroutineScope(Dispatchers.Default.limitedParallelism(1))
 *
 * Note that all calls to this machine instance should be done only from that thread.
 */
fun createCoStateMachine(
    scope: CoroutineScope,
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    start: Boolean = true,
    autoDestroyOnStatesReuse: Boolean = true,
    enableUndo: Boolean = false,
    doNotThrowOnMultipleTransitionsMatch: Boolean = false,
    init: BuildingStateMachine.() -> Unit
) = createStateMachine(
    name,
    childMode,
    start,
    autoDestroyOnStatesReuse,
    enableUndo,
    doNotThrowOnMultipleTransitionsMatch,
    CoroutinesLibCoroutineAbstraction(scope),
    init
)