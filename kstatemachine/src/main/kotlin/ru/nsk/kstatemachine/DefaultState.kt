package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.ChildMode.EXCLUSIVE
import ru.nsk.kstatemachine.HistoryType.DEEP
import ru.nsk.kstatemachine.HistoryType.SHALLOW

open class DefaultState(name: String? = null, childMode: ChildMode = EXCLUSIVE) :
    BaseStateImpl(name, childMode), State

open class DefaultDataState<out D : Any>(
    name: String? = null,
    override val defaultData: D? = null,
    childMode: ChildMode = EXCLUSIVE
) : BaseStateImpl(name, childMode), DataState<D> {
    private var _data: D? = null
    override val data: D get() = checkNotNull(_data) { "Data is not set. Is $this state active?" }

    private var _lastData: D? = null
    override val lastData: D
        get() = checkNotNull(_lastData ?: defaultData) {
            "Last data is not available yet in $this, and default data not provided"
        }

    override fun onDoEnter(transitionParams: TransitionParams<*>) {
        if (this == transitionParams.direction.targetState) {
            when (val event = transitionParams.event) {
                is DataEvent<*> -> assignEvent(event)
                is WrappedEvent -> assignEvent(event.event)
                else -> error("$event does not contain data required by $this")
            }
        } else { // implicit activation
            _data = lastData
        }
    }

    private fun assignEvent(event: Event) {
        @Suppress("UNCHECKED_CAST")
        event as DataEvent<D>
        with(event.data) {
            _data = this
            _lastData = this
        }
    }

    override fun onDoExit(transitionParams: TransitionParams<*>) {
        _data = null
    }

    override fun onStopped() {
        _data = null
        _lastData = null
    }

    override fun onCleanup() = onStopped()
}

open class DefaultFinalState(name: String? = null) : DefaultState(name), FinalState {
    override fun <E : Event> addTransition(transition: Transition<E>) = super<FinalState>.addTransition(transition)
}

open class DefaultFinalDataState<out D : Any>(name: String? = null, defaultData: D? = null) :
    DefaultDataState<D>(name, defaultData), FinalDataState<D> {
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

open class BasePseudoState(name: String?) : BaseStateImpl(name, EXCLUSIVE), PseudoState {
    override fun doEnter(transitionParams: TransitionParams<*>) = internalError()
    override fun doExit(transitionParams: TransitionParams<*>) = internalError()

    override fun <L : IState.Listener> addListener(listener: L) =
        throw UnsupportedOperationException("PseudoState $this can not have listeners")

    override fun <S : IState> addState(state: S, init: StateBlock<S>?) =
        throw UnsupportedOperationException("PseudoState $this can not have child states")


    override fun <E : Event> addTransition(transition: Transition<E>) =
        throw UnsupportedOperationException("PseudoState $this can not have transitions")

    private fun internalError(): Nothing =
        error("Internal error, PseudoState $this can not be entered or exited, looks that machine is purely configured")

}

/**
 * It is open for subclassing as all other [State] implementations, but I do not know real use cases for it.
 */
open class DefaultHistoryState(
    name: String? = null,
    private var _defaultState: IState? = null,
    final override val historyType: HistoryType = SHALLOW
) : BasePseudoState(name), HistoryState {
    override val defaultState get() = checkNotNull(_defaultState) { "Internal error, default state is not set" }

    private var _storedState: IState? = null
    override val storedState
        get() = (_storedState ?: defaultState).also {
            machine.log { "$this resolved to $it" }
        }

    override fun setParent(parent: InternalState) {
        super.setParent(parent)

        if (_defaultState != null)
            require(parent.states.contains(defaultState)) { "Default state $defaultState is not a neighbour of $this" }
        else
            _defaultState = parent.initialState
    }

    override fun onParentCurrentStateChanged(currentState: InternalState) {
        _storedState = currentState
    }

    override fun recursiveAfterTransitionComplete(transitionParams: TransitionParams<*>) {
        super.recursiveAfterTransitionComplete(transitionParams)
        if (historyType == DEEP) {
            transitionParams.direction.targetState?.let { targetState ->
                _storedState?.let {
                    if (targetState.isSubStateOf(it))
                        _storedState = targetState
                }
            }
        }
    }

    override fun onStopped() {
        _storedState = null
    }

    override fun onCleanup() {
        onStopped()
        _defaultState = null
    }
}