package ru.nsk.kstatemachine

import kotlinx.coroutines.CoroutineScope

internal class CoroutineProcessingEngine(val scope: CoroutineScope) : CoroutineStarter

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
    CoroutineProcessingEngine(scope),
    init
)