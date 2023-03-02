package ru.nsk.kstatemachine

import kotlin.UnsupportedOperationException
import kotlin.coroutines.*

/**
 * Starts coroutines in blocking and nonblocking way.
 * Mostly for internal use, there should be no reason to use it derectly in client code.
 */
interface CoroutineStarter {
    fun <R: Any> startBlocking(block: suspend () -> R): R
    fun startAsync(block: suspend () -> Unit)
}

internal class StdLibCoroutineStarter : CoroutineStarter {
    override fun <R : Any> startBlocking(block: suspend () -> R): R {
        lateinit var result: R
        suspend {
            result = block()
        }.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) = result.getOrThrow()
        })
        try {
            return result
        } catch (e: UninitializedPropertyAccessException) {
            throw UnsupportedOperationException(
                "Seems that you are trying to use Kotlin Coroutines library from KStateMachine callbacks, " +
                        "use kstatemachine-coroutines library for that " +
                        "and create your machine with createCoStateMachine() function", e
            )
        }
    }

    override fun startAsync(block: suspend () -> Unit) {
        throw UnsupportedOperationException(
            "${this::class.simpleName} does not support asynchronous start, " +
                    "use kstatemachine-coroutines library for that"
        )
    }
}