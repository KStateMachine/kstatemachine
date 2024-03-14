package ru.nsk.kstatemachine.coroutines

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

internal class CoroutinesLibCoroutineAbstraction(internal val scope: CoroutineScope) : CoroutineAbstraction {
    override fun <R : Any> runBlocking(block: suspend () -> R): R =
        doRunBlocking(scope.coroutineContext) { block() }

    /** Switches to context of the [scope] */
    override suspend fun <R : Any> withContext(block: suspend () -> R): R =
        withContext(scope.coroutineContext) { block() }
}

/**
 * Calls [kotlinx.coroutines.runBlocking] on supporting platforms, otherwise throws
 */
internal expect fun <T> doRunBlocking(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T