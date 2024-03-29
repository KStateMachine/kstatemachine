package ru.nsk.kstatemachine.statemachine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import ru.nsk.kstatemachine.coroutines.CoroutinesLibCoroutineAbstraction
import ru.nsk.kstatemachine.coroutines.createStateMachine
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.ChildMode

/**
 * Suspendable analog of [createStdLibStateMachine] function, with Kotlin Coroutines support.
 * This is preferred function. Use this one especially if you are going to use Kotlin Coroutines library from
 * KStateMachine callbacks.
 *
 * @param scope be careful while working with threaded scopes as KStateMachine classes are not thread-safe.
 * Usually you should use only single threaded scopes, for example:
 *
 *     CoroutineScope(newSingleThreadContext("single threaded context"))
 *
 * Note that all calls to created machine instance should be done only from that thread.
 */
suspend fun createStateMachine(
    scope: CoroutineScope,
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    start: Boolean = true,
    creationArguments: StateMachine.CreationArguments = StateMachine.CreationArguments(),
    init: suspend BuildingStateMachine.() -> Unit
) = CoroutinesLibCoroutineAbstraction(scope)
    .createStateMachine(name, childMode, start, creationArguments, init)

/**
 * Processes event in async fashion (using launch() to start new coroutine).
 *
 * This API requires [StateMachine]'s [CoroutineScope] so it throws if called on
 * machines created by [createStdLibStateMachine].
 */
fun StateMachine.processEventByLaunch(event: Event, argument: Any? = null) {
    val coroutineAbstraction = coroutineAbstraction
    require(coroutineAbstraction is CoroutinesLibCoroutineAbstraction) {
        "${::processEventByLaunch.name} API may be called on ${StateMachine::class.simpleName} " +
                "created with coroutines support only"
    }
    coroutineAbstraction.scope.launch { processEvent(event, argument) }
}

/**
 * Processes event in async fashion (using async() to start new coroutine) and returns result as [Deferred].
 *
 * This API requires [StateMachine]'s [CoroutineScope] so it throws if called on
 * machines created by [createStdLibStateMachine].
 */
fun StateMachine.processEventByAsync(event: Event, argument: Any? = null): Deferred<ProcessingResult> {
    val coroutineAbstraction = coroutineAbstraction
    require(coroutineAbstraction is CoroutinesLibCoroutineAbstraction) {
        "${::processEventByAsync.name} API may be called on ${StateMachine::class.simpleName} " +
                "created with coroutines support only"
    }
    return coroutineAbstraction.scope.async { processEvent(event, argument) }
}