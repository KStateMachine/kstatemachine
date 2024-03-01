package ru.nsk.kstatemachine.state.pseudo

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.transition.Transition
import ru.nsk.kstatemachine.transition.TransitionParams

open class BasePseudoState(name: String?) : BaseStateImpl(name, ChildMode.EXCLUSIVE), PseudoState {
    override suspend fun doEnter(transitionParams: TransitionParams<*>) = internalError()
    override suspend fun doExit(transitionParams: TransitionParams<*>) = internalError()

    override fun <L : IState.Listener> addListener(listener: L) =
        throw UnsupportedOperationException("PseudoState $this can not have listeners")

    override fun <S : IState> addState(state: S, init: StateBlock<S>?) =
        throw UnsupportedOperationException("PseudoState $this can not have child states")


    override fun <E : Event> addTransition(transition: Transition<E>) =
        throw UnsupportedOperationException("PseudoState $this can not have transitions")

    private fun internalError(): Nothing =
        error("Internal error, PseudoState $this can not be entered or exited, looks that machine is purely configured")
}