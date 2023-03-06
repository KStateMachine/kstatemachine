package ru.nsk.kstatemachine

private data class StateAndEvent(val state: IState, val eventAndArgument: EventAndArgument<*>)

internal class UndoState : BasePseudoState("undo") {
    private val stack = mutableListOf<StateAndEvent>()

    override suspend fun recursiveAfterTransitionComplete(transitionParams: TransitionParams<*>) {
        super.recursiveAfterTransitionComplete(transitionParams)
        if (transitionParams.event !is WrappedEvent) { // do not record self-made transition
            // check target-less transition
            val targetState = transitionParams.direction.targetState ?: transitionParams.transition.sourceState
            stack += StateAndEvent(targetState, EventAndArgument(transitionParams.event, transitionParams.argument))
        }
    }

    /**
     * Called before [popState]
     */
    fun makeWrappedEvent(): WrappedEvent {
        val element = stack.getOrNull(stack.size - 2)
        return if (element != null)
            WrappedEvent(element.eventAndArgument.event, element.eventAndArgument.argument)
        else
            WrappedEvent(UndoEvent, null)
    }

    fun popState() = if (stack.size >= 2) {
        stack.removeLast()
        stack.last().state
    } else {
        null
    }

    override suspend fun onStopped() = stack.clear()
    override suspend fun onCleanup() = stack.clear()
}