package ru.nsk.samples

import kotlin.coroutines.*


fun main() {

    println("start it ${Thread.currentThread()}")
    lateinit var context: Continuation<Unit>

    val block = suspend {
        val extra="extra"
        println("before suspend $extra  ${Thread.currentThread()}")
        //suspendCoroutine { context = it }
        println("after suspend $extra  ${Thread.currentThread()}")
    }

    println("startCoroutine it ${Thread.currentThread()}")
    block.startCoroutine(
        object : Continuation<Unit> {
            override val context: CoroutineContext = EmptyCoroutineContext
            // called when a coroutine ends. do nothing.
            override fun resumeWith(result: Result<Unit>) {
                result.onFailure { ex : Throwable -> throw ex }
            }
        }
    )
    println("after startCoroutine it ${Thread.currentThread()}")

    println("kick it  ${Thread.currentThread()}")
    //context.resume(Unit)

    println("kick end  ${Thread.currentThread()}")
}