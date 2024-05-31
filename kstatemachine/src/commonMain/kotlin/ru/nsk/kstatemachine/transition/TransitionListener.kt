/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.transition

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.statemachine.InternalStateMachine
import ru.nsk.kstatemachine.statemachine.runDelayingException

internal suspend inline fun InternalTransition<*>.transitionNotify(
    crossinline block: suspend Transition.Listener.() -> Unit
) {
    val machine = sourceState.machine as InternalStateMachine
    if (machine.areListenersMuted) return
    listeners.toList().forEach { machine.runDelayingException { it.block() } }
}

inline fun <reified E : Event> Transition<E>.onTriggered(
    crossinline block: suspend (TransitionParams<E>) -> Unit
) = addListener(object : Transition.Listener {
    @Suppress("UNCHECKED_CAST")
    override suspend fun onTriggered(transitionParams: TransitionParams<*>) =
        block(transitionParams as TransitionParams<E>)
})

inline fun <reified E : Event> Transition<E>.onComplete(
    crossinline block: suspend (activeStates: Set<IState>, TransitionParams<E>) -> Unit
) = addListener(object : Transition.Listener {
    @Suppress("UNCHECKED_CAST")
    override suspend fun onComplete(activeStates: Set<IState>, transitionParams: TransitionParams<*>) =
        block(activeStates, transitionParams as TransitionParams<E>)
})