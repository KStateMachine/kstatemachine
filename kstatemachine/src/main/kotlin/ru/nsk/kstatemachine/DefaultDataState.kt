package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.TransitionDirectionProducerPolicy.DefaultPolicy
import ru.nsk.kstatemachine.TreeAlgorithms.findPathFromTargetToLca
import java.util.concurrent.CopyOnWriteArraySet

open class DefaultState(name: String? = null) : BaseStateImpl(name), State

open class DefaultDataState<out D>(name: String? = null) : BaseStateImpl(name), DataState<D> {
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
}

open class BaseStateImpl(override val name: String?) : InternalState {
    private val _listeners = CopyOnWriteArraySet<IState.Listener>()
    override val listeners: Collection<IState.Listener> get() = _listeners

    private val _states = mutableSetOf<InternalState>()
    override val states: Set<IState> get() = _states

    /**
     * Might be null only before [setInitialState] call.
     */
    protected var currentState: InternalState? = null

    private var _initialState: InternalState? = null
    override val initialState get() = _initialState

    override var parent: InternalState? = null

    override val machine get() = if (this is StateMachine) this else requireParent().machine

    private val _transitions = mutableSetOf<Transition<*>>()
    override val transitions: Set<Transition<*>> get() = _transitions

    private var _isActive = false
    override val isActive get() = _isActive

    private var isFinished = false

    override fun <L : IState.Listener> addListener(listener: L): L {
        require(_listeners.add(listener)) { "$listener is already added" }
        return listener
    }

    override fun removeListener(listener: IState.Listener) {
        _listeners.remove(listener)
    }

    override fun <S : IState> addState(state: S, init: StateBlock<S>?): S {
        check(!machine.isRunning) { "Can not add state after state machine started" }

        state.name?.let {
            require(findState(it) == null) { "State with name $it already exists" }
        }

        state as InternalState
        require(_states.add(state)) { "$state already added" }
        state.parent = this
        if (init != null) state.init()
        return state
    }

    override fun setInitialState(state: IState) {
        require(states.contains(state)) { "$state is not part of $this machine, use addState() first" }
        check(!machine.isRunning) { "Can not change initial state after state machine started" }

        _initialState = state as InternalState
    }

    override fun <E : Event> addTransition(transition: Transition<E>): Transition<E> {
        _transitions += transition
        return transition
    }

    override fun toString() = "${this::class.simpleName}(name=$name)"

    override fun asState() = this

    protected open fun onDoEnter(transitionParams: TransitionParams<*>) {
        /* default empty */
    }

    protected open fun onDoExit(transitionParams: TransitionParams<*>) {
        /* default empty */
    }

    override fun doEnter(transitionParams: TransitionParams<*>) {
        if (!_isActive) {
            machine.log("Parent $parent entering child $this")
            _isActive = true
            onDoEnter(transitionParams)
            stateNotify { onEntry(transitionParams) }
        }
    }

    override fun doExit(transitionParams: TransitionParams<*>) {
        if (_isActive) {
            machine.log("Exiting $this")
            onDoExit(transitionParams)
            _isActive = false
            stateNotify { onExit(transitionParams) }
        }
    }

    override fun doProcessEvent(event: Event, argument: Any?): Boolean {
        val machine = machine as InternalStateMachine

        if (isFinished) {
            machine.log("$this is finished, skipping event $event, with argument $argument")
            return false
        }

        val (transition, direction) = recursiveFindUniqueTransitionWithDirection(event) ?: return false

        val transitionParams = TransitionParams(transition, direction, event, argument)

        val targetState = direction.targetState as? InternalState

        if (direction !is NoTransition) {
            machine.log("$this triggering $transition from ${transition.sourceState}")
            transition.transitionNotify { onTriggered(transitionParams) }

            machine.machineNotify { onTransition(transition.sourceState, targetState, event, argument) }
        }

        targetState?.let { switchToTargetState(it, transition.sourceState, transitionParams) }
        return true
    }

    override fun <E : Event> recursiveFindUniqueTransitionWithDirection(event: E):
            Pair<InternalTransition<E>, TransitionDirection>? {
        return currentState?.recursiveFindUniqueTransitionWithDirection(event)
            ?: findUniqueTransitionWithDirection(event)
    }

    override fun recursiveEnterInitialState() {
        if (states.isEmpty()) return

        val initialState = checkNotNull(initialState) { "Initial state is not set, call setInitialState() first" }

        setCurrentState(initialState, makeStartTransitionParams(initialState))

        initialState.recursiveEnterInitialState()
    }

    override fun recursiveEnterStatePath(path: MutableList<InternalState>, transitionParams: TransitionParams<*>) {
        if (path.isEmpty()) {
            recursiveEnterInitialState()
        } else {
            val state = path.removeLast()
            setCurrentState(state, transitionParams)

            if (state !is StateMachine) // inner state machine manages its internal state by its own
                state.recursiveEnterStatePath(path, transitionParams)
        }
    }

    override fun recursiveExit(transitionParams: TransitionParams<*>) {
        if (states.isNotEmpty())
            requireCurrentState().recursiveExit(transitionParams)

        doExit(transitionParams)
    }

    override fun recursiveStop() {
        currentState = null
        _isActive = false
        isFinished = false
        _states.forEach { it.recursiveStop() }
    }

    override fun recursiveFillActiveStates(states: MutableSet<IState>) {
        if (isActive) {
            states.add(this)

            val currentState = currentState
            // do not include nested state machine states
            if (currentState is StateMachine)
                states.add(currentState)
            else
                currentState?.recursiveFillActiveStates(states)
        }
    }

    private fun requireCurrentState() = requireNotNull(currentState) { "Current state is not set" }

    private fun setCurrentState(state: InternalState, transitionParams: TransitionParams<*>) {
        if (currentState == state) return

        currentState?.recursiveExit(transitionParams)

        val machine = machine as InternalStateMachine
        require(states.contains(state)) { "$state is not a child of $this" }

        currentState = state

        val finish = state is IFinalState
        if (finish) isFinished = true

        state.doEnter(transitionParams)

        if (finish) {
            machine.log("Parent $this finish")
            stateNotify { onFinished() }
        }

        machine.machineNotify { onStateChanged(state) }
    }

    internal fun switchToTargetState(
        targetState: InternalState,
        fromState: InternalState,
        transitionParams: TransitionParams<*>
    ) {
        val path = fromState.findPathFromTargetToLca(targetState)
        val lca = path.removeLast()
        lca.recursiveEnterStatePath(path, transitionParams)
    }

    /**
     * Initial event which is processed on state machine start
     */
    internal object StartEvent : Event

    internal fun makeStartTransitionParams(
        sourceState: IState,
        targetState: IState = sourceState
    ): TransitionParams<*> {
        val transition = DefaultTransition(
            "Starting",
            EventMatcher.isInstanceOf<StartEvent>(),
            sourceState,
            targetState,
        )

        return TransitionParams(
            transition,
            transition.produceTargetStateDirection(DefaultPolicy(StartEvent)),
            StartEvent,
        )
    }
}

open class DefaultFinalDataState<out D>(name: String? = null) : DefaultDataState<D>(name), FinalDataState<D> {
    override fun <E : Event> addTransition(transition: Transition<E>) =
        throw UnsupportedOperationException("FinalState can not have transitions")
}

open class DefaultFinalState(name: String?) : DefaultState(name), FinalState {
    override fun <E : Event> addTransition(transition: Transition<E>) =
        throw UnsupportedOperationException("FinalState can not have transitions")
}
