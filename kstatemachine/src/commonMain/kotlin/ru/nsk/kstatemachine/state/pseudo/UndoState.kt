package ru.nsk.kstatemachine.state.pseudo

import ru.nsk.kstatemachine.event.UndoEvent
import ru.nsk.kstatemachine.event.WrappedEvent
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.transition.EventAndArgument
import ru.nsk.kstatemachine.transition.TransitionParams

private data class StateAndEvent(val states: Set<IState>, val eventAndArgument: EventAndArgument<*>)

internal class UndoState : BasePseudoState("undoState") {
    private val stack = mutableListOf<StateAndEvent>()

    override suspend fun recursiveAfterTransitionComplete(transitionParams: TransitionParams<*>) {
        super.recursiveAfterTransitionComplete(transitionParams)
        if (transitionParams.event !is WrappedEvent) { // do not record self-made transition
            // check target-less transition
            val targetStates = transitionParams.direction.targetStates.takeIf { it.isNotEmpty() }
                ?: setOf(transitionParams.transition.sourceState)
            stack += StateAndEvent(targetStates, EventAndArgument(transitionParams.event, transitionParams.argument))
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

    fun popState(): Set<IState> = if (stack.size >= 2) {
        stack.removeLast()
        stack.last().states
    } else {
        emptySet()
    }

    override suspend fun onStopped(): Unit = stack.clear()
    override suspend fun onCleanup(): Unit = stack.clear()
}