package ru.nsk.kstatemachine

import kotlinx.coroutines.CoroutineScope

/**
 * Analog of [createStateMachine] function, with kotlin coroutines support.
 * Use this one if you are going to use kotlin coroutines library from KStateMachine callbacks.
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
    CoroutinesLibCoroutineStarter(scope),
    init
)