/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.coroutines

import ru.nsk.kstatemachine.state.ChildMode
import ru.nsk.kstatemachine.statemachine.*
import ru.nsk.kstatemachine.statemachine.StateMachineImpl
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * Starts coroutines in blocking and nonblocking way.
 * Mostly for internal use, there should be no reason to use it directly in client code.
 */
interface CoroutineAbstraction {
    fun <R : Any> runBlocking(block: suspend () -> R): R
    suspend fun <R : Any> withContext(block: suspend () -> R): R
}

/**
 * Standard library implementation. It should be used only if you don't want to depend on Kotlin Coroutines library.
 * This makes possible to have suspendable methods in KStateMachine interfaces and
 * not to depend on Kotlin Coroutines library.
 */
internal class StdLibCoroutineAbstraction : CoroutineAbstraction {
    /** Just starts coroutine */
    override fun <R : Any> runBlocking(block: suspend () -> R): R {
        lateinit var result: R
        suspend {
            result = block()
        }.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) = result.getOrThrow()
        })
        try {
            return result
        } catch (e: Exception) {
            throw IllegalStateException(
                "Seems that you are trying to use Kotlin Coroutines library from KStateMachine callbacks, " +
                        "use kstatemachine-coroutines support library to make that work", e
            )
        }
    }

    /** Simply calls [block] */
    override suspend fun <R : Any> withContext(block: suspend () -> R): R = block()
}

@OptIn(ExperimentalContracts::class)
suspend fun CoroutineAbstraction.createStateMachine(
    name: String?,
    childMode: ChildMode,
    start: Boolean,
    creationArguments: CreationArguments = buildCreationArguments {},
    init: suspend BuildingStateMachine.() -> Unit
): StateMachine {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return StateMachineImpl(
        name,
        childMode,
        creationArguments,
        this,
    ).apply {
        init()
        if (start) start()
    }
}