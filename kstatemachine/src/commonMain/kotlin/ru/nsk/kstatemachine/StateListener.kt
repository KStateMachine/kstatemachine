package ru.nsk.kstatemachine

internal suspend inline fun InternalState.stateNotify(crossinline block: suspend IState.Listener.() -> Unit) {
    val machine = machine as InternalStateMachine
    listeners.toList().forEach { machine.runDelayingException { it.block() } }
}

/**
 * The most commonly used methods [onEntry] and [onExit] are shipped with [once] argument, to remove listener
 * after it is triggered the first time.
 * Looks that it is not necessary in other similar methods.
 */
inline fun <S : IState> S.onEntry(once: Boolean = false, crossinline block: suspend S.(TransitionParams<*>) -> Unit) =
    addListener(object : IState.Listener {
        override suspend fun onEntry(transitionParams: TransitionParams<*>) {
            block(transitionParams)
            if (once) removeListener(this)
        }
    })

/** See [onEntry] */
inline fun <S : IState> S.onExit(once: Boolean = false, crossinline block: suspend S.(TransitionParams<*>) -> Unit) =
    addListener(object : IState.Listener {
        override suspend fun onExit(transitionParams: TransitionParams<*>) {
            block(transitionParams)
            if (once) removeListener(this)
        }
    })

inline fun <S : IState> S.onFinished(crossinline block: suspend S.(TransitionParams<*>) -> Unit) =
    addListener(object : IState.Listener {
        override suspend fun onFinished(transitionParams: TransitionParams<*>) = block(transitionParams)
    })