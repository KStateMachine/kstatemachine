/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.nsk.kstatemachine.coroutines.CoroutinesLibCoroutineAbstraction
import ru.nsk.kstatemachine.statemachine.StateMachine

/**
 * Launches a coroutine scoped to the state's active lifetime — a UML **do activity**.
 *
 * The coroutine starts when the state is entered (after its entry actions complete) and is
 * automatically cancelled when the state is exited. It is re-launched on every subsequent re-entry.
 * The job is also cancelled when the machine is stopped or destroyed, even if a normal state exit
 * transition did not occur (machine stop does not trigger [IState.Listener.onExit]).
 *
 * Typical usage — run work until the state is left:
 * ```kotlin
 * state("polling") {
 *     asyncScopedAction {
 *         while (true) {
 *             pollServer()
 *             delay(1_000)
 *         }
 *     }
 * }
 * ```
 *
 * Requires a machine created with [createStateMachine] (coroutines support).
 * Throws [IllegalArgumentException] if called on a machine created with [createStdLibStateMachine].
 */
fun <S : IState> S.asyncScopedAction(block: suspend S.() -> Unit) {
    val abstraction = machine.coroutineAbstraction
    require(abstraction is CoroutinesLibCoroutineAbstraction) {
        "asyncScopedAction requires a StateMachine created with coroutines support (createStateMachine)"
    }
    var job: Job? = null
    fun cancelAndClearJob() {
        job?.let {
            it.cancel()
            job = null
        }
    }
    // machine.stop() and machine.destroy() do not trigger IState.Listener.onExit, so register
    // a machine-level listener once at setup time to cancel the job in those cases.
    // job is null when the state is inactive, so this is a no-op for all other states.
    machine.addListener(object : StateMachine.Listener {
        override suspend fun onStopped() = cancelAndClearJob()
        override suspend fun onDestroyed() = cancelAndClearJob()
    })

    onEntry {
        check(job == null) { "Job is already running - this is logical error, please report a bug" }
        job = abstraction.scope.launch { block() }
    }
    onExit {
        cancelAndClearJob()
    }
}
