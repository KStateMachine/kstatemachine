package ru.nsk.kstatemachine

private data class StateAndEvent(val state: IState, val event: Event)

internal class UndoState : BasePseudoState("undo") {
    private val stack = mutableListOf<StateAndEvent>()

    override fun recursiveAfterTransitionComplete(transitionParams: TransitionParams<*>) {
        super.recursiveAfterTransitionComplete(transitionParams)
        if (transitionParams.event !is IUndoEvent) { // do not record self-made transition
            transitionParams.direction.targetState?.let {
                stack += StateAndEvent(it, transitionParams.event)
            }
        }
    }

    fun popState(): IState? {
        stack.removeLastOrNull()
        return stack.lastOrNull()?.state
    }

    fun makeReachUndoEvent(): IUndoEvent {
        val event = stack.getOrNull(stack.size - 2)?.event
        return if (event is DataEvent<*>) UndoDataEvent(event.data) else UndoEvent
    }

    override fun onStopped() = stack.clear()
    override fun onCleanup() = onStopped()
}