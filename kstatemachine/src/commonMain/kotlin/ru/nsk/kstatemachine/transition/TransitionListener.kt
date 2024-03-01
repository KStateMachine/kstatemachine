package ru.nsk.kstatemachine.transition

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.statemachine.InternalStateMachine
import ru.nsk.kstatemachine.statemachine.runDelayingException

internal suspend inline fun InternalTransition<*>.transitionNotify(
    crossinline block: suspend Transition.Listener.() -> Unit
) {
    val machine = sourceState.machine as InternalStateMachine
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
    crossinline block: suspend (TransitionParams<E>, Set<IState>) -> Unit
) = addListener(object : Transition.Listener {
    @Suppress("UNCHECKED_CAST")
    override suspend fun onComplete(transitionParams: TransitionParams<*>, activeStates: Set<IState>) =
        block(transitionParams as TransitionParams<E>, activeStates)
})