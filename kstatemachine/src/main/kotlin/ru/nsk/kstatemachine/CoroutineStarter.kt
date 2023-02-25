package ru.nsk.kstatemachine

import kotlin.coroutines.*

interface CoroutineStarter {
    fun <R: Any> start(block: suspend () -> R): R
}

internal class StdLibCoroutineStarter : CoroutineStarter {
    override fun <R : Any> start(block: suspend () -> R): R {
        lateinit var result: R
        println("Starter start ${Thread.currentThread()}")
        suspend {
            println("Starter in suspend ${Thread.currentThread()}")
            result = block()
            println("Starter out suspend ${Thread.currentThread()}")
        }.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) = result.getOrThrow()
        })
        println("Starter return ${Thread.currentThread()}")
        return result
    }
}