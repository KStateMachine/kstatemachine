package ru.nsk.kstatemachine

import kotlinx.coroutines.*

internal class CoroutinesLibCoroutineStarter(private val scope: CoroutineScope) : CoroutineStarter {
    override fun <R : Any> start(block: suspend () -> R): R {
        return runBlocking {
            withContext(scope.coroutineContext) {
                block()
            }
        }
    }
}

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