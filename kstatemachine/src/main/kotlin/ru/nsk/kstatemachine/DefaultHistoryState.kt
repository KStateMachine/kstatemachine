package ru.nsk.kstatemachine

/**
 * It is open for subclassing as all other [State] implementations, but I do not know real use cases for it.
 */
open class DefaultHistoryState(
    name: String? = null,
    private var _defaultState: IState? = null,
    final override val historyType: HistoryType = HistoryType.SHALLOW
) : BasePseudoState(name), HistoryState {
    override val defaultState get() = checkNotNull(_defaultState) { "Internal error, default state is not set" }

    private var _storedState: IState? = null
    override val storedState get() = _storedState ?: defaultState

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

    override suspend fun recursiveAfterTransitionComplete(transitionParams: TransitionParams<*>) {
        super.recursiveAfterTransitionComplete(transitionParams)
        if (historyType == HistoryType.DEEP) {
            transitionParams.direction.targetState?.let { targetState ->
                _storedState?.let {
                    if (targetState.isSubStateOf(it))
                        _storedState = targetState
                }
            }
        }
    }

    override suspend fun onStopped() {
        _storedState = null
    }

    override suspend fun onCleanup() {
        _storedState = null
        _defaultState = null
    }
}