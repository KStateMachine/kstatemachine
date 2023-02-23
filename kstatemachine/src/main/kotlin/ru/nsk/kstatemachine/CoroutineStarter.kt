package ru.nsk.kstatemachine

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

interface CoroutineStarter {
    fun <R: Any> start(block: suspend () -> R): R
}

internal class StdLibCoroutineStarter : CoroutineStarter {
    override fun <R : Any> start(block: suspend () -> R): R {
        lateinit var result: R
        suspend {
            result = block()
        }.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) = result.getOrThrow()
        })
        return result
    }
}