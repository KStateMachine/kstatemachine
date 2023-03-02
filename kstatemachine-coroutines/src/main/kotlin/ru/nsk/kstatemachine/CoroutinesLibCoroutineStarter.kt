package ru.nsk.kstatemachine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

internal class CoroutinesLibCoroutineStarter(private val scope: CoroutineScope) : CoroutineStarter {
    override fun <R : Any> startBlocking(block: suspend () -> R) =
        runBlocking(scope.coroutineContext) { block() }

    override fun startAsync(block: suspend () -> Unit) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { block() }
    }
}