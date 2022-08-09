package ru.nsk.kstatemachine

internal class UndoState : BasePseudoState("undo") {
    private val stack = mutableListOf<IState>()

    override fun recursiveAfterTransitionComplete(transitionParams: TransitionParams<*>) {
        super.recursiveAfterTransitionComplete(transitionParams)
        if (transitionParams.event !is UndoEvent) { // do not record self-made transition
            transitionParams.direction.targetState?.let {
                stack += it
            }
        }
    }

    fun popState() = stack.removeLastOrNull()

    override fun onStopped() = stack.clear()
    override fun onCleanup() = onStopped()
}