package ru.nsk.kstatemachine

internal suspend inline fun InternalStateMachine.machineNotify(crossinline block: suspend StateMachine.Listener.() -> Unit) {
    machineListeners.toList().forEach { runDelayingException { it.block() } }
}

inline fun StateMachine.onStarted(crossinline block: suspend StateMachine.() -> Unit) =
    addListener(object : StateMachine.Listener {
        override suspend fun onStarted() = block()
    })

inline fun StateMachine.onStopped(crossinline block: suspend StateMachine.() -> Unit) =
    addListener(object : StateMachine.Listener {
        override suspend fun onStopped() = block()
    })

inline fun StateMachine.onTransition(crossinline block: suspend StateMachine.(TransitionParams<*>) -> Unit) =
    addListener(object : StateMachine.Listener {
        override suspend fun onTransition(transitionParams: TransitionParams<*>) =
            block(transitionParams)
    })

inline fun StateMachine.onTransitionComplete(crossinline block: suspend StateMachine.(TransitionParams<*>, Set<IState>) -> Unit) =
    addListener(object : StateMachine.Listener {
        override suspend fun onTransitionComplete(transitionParams: TransitionParams<*>, activeStates: Set<IState>) =
            block(transitionParams, activeStates)
    })

inline fun StateMachine.onStateEntry(crossinline block: suspend StateMachine.(state: IState) -> Unit) =
    addListener(object : StateMachine.Listener {
        override suspend fun onStateEntry(state: IState) = block(state)
    })