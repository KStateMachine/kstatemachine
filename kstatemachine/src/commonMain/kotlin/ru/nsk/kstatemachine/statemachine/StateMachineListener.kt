package ru.nsk.kstatemachine.statemachine

import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.transition.TransitionParams

internal suspend inline fun StateMachine.machineNotify(crossinline block: suspend StateMachine.Listener.() -> Unit) {
    this as InternalStateMachine
    if (areListenersMuted) return
    machineListeners.toList().forEach { runDelayingException { it.block() } }
}

inline fun StateMachine.onStarted(crossinline block: suspend StateMachine.(TransitionParams<*>) -> Unit) =
    addListener(object : StateMachine.Listener {
        override suspend fun onStarted(transitionParams: TransitionParams<*>) = block(transitionParams)
    })

inline fun StateMachine.onTransitionTriggered(crossinline block: suspend StateMachine.(TransitionParams<*>) -> Unit) =
    addListener(object : StateMachine.Listener {
        override suspend fun onTransitionTriggered(transitionParams: TransitionParams<*>) =
            block(transitionParams)
    })

inline fun StateMachine.onTransitionComplete(
    crossinline block: suspend StateMachine.(activeStates: Set<IState>, TransitionParams<*>) -> Unit
) = addListener(object : StateMachine.Listener {
    override suspend fun onTransitionComplete(activeStates: Set<IState>, transitionParams: TransitionParams<*>) =
        block(activeStates, transitionParams)
})

inline fun StateMachine.onStateEntry(
    crossinline block: suspend StateMachine.(IState, TransitionParams<*>) -> Unit
) = addListener(object : StateMachine.Listener {
    override suspend fun onStateEntry(state: IState, transitionParams: TransitionParams<*>) =
        block(state, transitionParams)
})

inline fun StateMachine.onStateExit(
    crossinline block: suspend StateMachine.(IState, TransitionParams<*>) -> Unit
) = addListener(object : StateMachine.Listener {
    override suspend fun onStateExit(state: IState, transitionParams: TransitionParams<*>) =
        block(state, transitionParams)
})

inline fun StateMachine.onStateFinished(
    crossinline block: suspend StateMachine.(IState, TransitionParams<*>) -> Unit
) = addListener(object : StateMachine.Listener {
    override suspend fun onStateFinished(state: IState, transitionParams: TransitionParams<*>) =
        block(state, transitionParams)
})

inline fun StateMachine.onStopped(crossinline block: suspend StateMachine.() -> Unit) =
    addListener(object : StateMachine.Listener {
        override suspend fun onStopped() = block()
    })

inline fun StateMachine.onDestroyed(crossinline block: suspend StateMachine.() -> Unit) =
    addListener(object : StateMachine.Listener {
        override suspend fun onDestroyed() = block()
    })