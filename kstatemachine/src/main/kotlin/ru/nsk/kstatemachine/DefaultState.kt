package ru.nsk.kstatemachine

open class DefaultState(name: String? = null, childMode: ChildMode = ChildMode.EXCLUSIVE) :
    BaseStateImpl(name, childMode), State

open class DefaultDataState<out D>(name: String? = null, childMode: ChildMode = ChildMode.EXCLUSIVE) :
    BaseStateImpl(name, childMode), DataState<D> {
    private var _data: D? = null
    override val data: D get() = checkNotNull(_data) { "Data is not set. Is the state active?" }

    override fun onDoEnter(transitionParams: TransitionParams<*>) {
        if (this == transitionParams.direction.targetState) {
            @Suppress("UNCHECKED_CAST")
            val event = transitionParams.event as? DataEvent<D>
            checkNotNull(event) { "${transitionParams.event} does not contain data required by $this" }
            _data = event.data
        } else {
            error(
                "$this is implicitly activated, this might be a result of a cross-level transition. " +
                        "Currently there is no way to get data for this state."
            )
        }
    }

    override fun onDoExit(transitionParams: TransitionParams<*>) {
        _data = null
    }

    override fun onCleanup() {
        _data = null
    }
}

open class DefaultFinalState(name: String? = null) : DefaultState(name), FinalState {
    override fun <E : Event> addTransition(transition: Transition<E>) = super<FinalState>.addTransition(transition)
}

open class DefaultFinalDataState<out D>(name: String? = null) : DefaultDataState<D>(name), FinalDataState<D> {
    override fun <E : Event> addTransition(transition: Transition<E>) = super<FinalDataState>.addTransition(transition)
}

/**
 * Currently it does not allow to target [DataState]
 */
open class DefaultChoiceState(name: String? = null, private val choiceAction: EventAndArgument<*>.() -> State) :
    BasePseudoState(name), RedirectPseudoState {

    override fun resolveTargetState(eventAndArgument: EventAndArgument<*>) =
        eventAndArgument.choiceAction().also { machine.log { "$this resolved to $it" } }
}

open class BasePseudoState(name: String?) : BaseStateImpl(name, ChildMode.EXCLUSIVE), PseudoState {
    override fun doEnter(transitionParams: TransitionParams<*>) = internalError()
    override fun doExit(transitionParams: TransitionParams<*>) = internalError()

    override fun <L : IState.Listener> addListener(listener: L) =
        throw UnsupportedOperationException("PseudoState can not have listeners")

    override fun <S : IState> addState(state: S, init: StateBlock<S>?) =
        throw UnsupportedOperationException("PseudoState can not have child states")


    override fun <E : Event> addTransition(transition: Transition<E>) =
        throw UnsupportedOperationException("PseudoState can not have transitions")

    private fun internalError(): Nothing =
        error("Internal error PseudoState can not be entered or exited, looks that machine is purely configured")

}