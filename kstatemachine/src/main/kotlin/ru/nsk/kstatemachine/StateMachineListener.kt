package ru.nsk.kstatemachine

internal suspend inline fun StateMachine.machineNotify(crossinline block: suspend StateMachine.Listener.() -> Unit) {
    this as InternalStateMachine
    machineListeners.toList().forEach { runDelayingException { it.block() } }
}

inline fun StateMachine.onStarted(crossinline block: suspend StateMachine.() -> Unit) =
    addListener(object : StateMachine.Listener {
        override suspend fun onStarted() = block()
    })

inline fun StateMachine.onTransitionTriggered(crossinline block: suspend StateMachine.(TransitionParams<*>) -> Unit) =
    addListener(object : StateMachine.Listener {
        override suspend fun onTransitionTriggered(transitionParams: TransitionParams<*>) =
            block(transitionParams)
    })

inline fun StateMachine.onTransitionComplete(
    crossinline block: suspend StateMachine.(transitionParams: TransitionParams<*>, activeStates: Set<IState>) -> Unit
) = addListener(object : StateMachine.Listener {
    override suspend fun onTransitionComplete(transitionParams: TransitionParams<*>, activeStates: Set<IState>) =
        block(transitionParams, activeStates)
})

inline fun StateMachine.onStateEntry(
    crossinline block: suspend StateMachine.(state: IState, transitionParams: TransitionParams<*>) -> Unit
) = addListener(object : StateMachine.Listener {
    override suspend fun onStateEntry(state: IState, transitionParams: TransitionParams<*>) =
        block(state, transitionParams)
})

inline fun StateMachine.onStateExit(
    crossinline block: suspend StateMachine.(state: IState, transitionParams: TransitionParams<*>) -> Unit
) = addListener(object : StateMachine.Listener {
    override suspend fun onStateExit(state: IState, transitionParams: TransitionParams<*>) =
        block(state, transitionParams)
})

inline fun StateMachine.onStateFinished(
    crossinline block: suspend StateMachine.(state: IState, transitionParams: TransitionParams<*>) -> Unit
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