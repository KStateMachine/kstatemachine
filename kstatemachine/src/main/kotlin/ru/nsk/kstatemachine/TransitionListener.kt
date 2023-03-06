package ru.nsk.kstatemachine

internal sealed class TransitionNotification {
    internal class OnTriggered(val transitionParams: TransitionParams<*>) : TransitionNotification()
}

internal suspend inline fun InternalTransition<*>.transitionNotify(crossinline block: suspend Transition.Listener.() -> Unit) {
    val machine = sourceState.machine as InternalStateMachine
    listeners.toList().forEach { machine.runDelayingException { it.block() } }
}

inline fun <reified E : Event> Transition<E>.onTriggered(
    crossinline block: suspend (TransitionParams<E>) -> Unit
) = addListener(object : Transition.Listener {
    @Suppress("UNCHECKED_CAST")
    override suspend fun onTriggered(transitionParams: TransitionParams<*>) = block(transitionParams as TransitionParams<E>)
})