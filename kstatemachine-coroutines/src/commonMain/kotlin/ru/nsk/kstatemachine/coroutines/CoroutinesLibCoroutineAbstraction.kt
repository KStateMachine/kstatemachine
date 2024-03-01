package ru.nsk.kstatemachine.coroutines

import kotlinx.coroutines.*

internal class CoroutinesLibCoroutineAbstraction(private val scope: CoroutineScope) : CoroutineAbstraction {
    /**
     * Calls [kotlinx.coroutines.runBlocking]
     */
    override fun <R : Any> runBlocking(block: suspend () -> R) =
        runBlocking(scope.coroutineContext) { block() }

    /** Switches to context of the [scope] */
    override suspend fun <R : Any> withContext(block: suspend () -> R) : R =
        withContext(scope.coroutineContext) { block() }
}