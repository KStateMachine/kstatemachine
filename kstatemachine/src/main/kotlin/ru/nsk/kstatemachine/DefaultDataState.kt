package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.TransitionDirectionProducerPolicy.DefaultPolicy
import ru.nsk.kstatemachine.TreeAlgorithms.findPathFromTargetToLca
import java.util.concurrent.CopyOnWriteArraySet

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
}

open class BaseStateImpl(override val name: String?, override val childMode: ChildMode) : InternalState {
    private val _listeners = CopyOnWriteArraySet<IState.Listener>()
    override val listeners: Collection<IState.Listener> get() = _listeners

    private val _states = mutableSetOf<InternalState>()
    override val states: Set<IState> get() = _states

    /**
     * In [ChildMode.EXCLUSIVE] might be null only before [setInitialState] call if there are child states.
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

    private var _isFinished = false
    override val isFinished get() = _isFinished

    override fun <L : IState.Listener> addListener(listener: L): L {
        require(_listeners.add(listener)) { "$listener is already added" }
        return listener
    }

    override fun removeListener(listener: IState.Listener) {
        _listeners.remove(listener)
    }

    override fun <S : IState> addState(state: S, init: StateBlock<S>?): S {
        check(!machine.isRunning) { "Can not add state after state machine started" }
        if (childMode == ChildMode.PARALLEL)
            require(state !is FinalState) { "Can not add FinalState in parallel child mode" }

        state.name?.let {
            require(findState(it, recursive = false) == null) { "State with name $it already exists" }
        }

        state as InternalState
        require(_states.add(state)) { "$state already added" }
        state.parent = this
        if (init != null) state.init()
        return state
    }

    override fun setInitialState(state: IState) {
        require(states.contains(state)) { "$state is not part of $this machine, use addState() first" }
        check(childMode == ChildMode.EXCLUSIVE) { "Can not set initial state in parallel child mode" }
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

    override fun afterChildFinished(state: InternalState) {
        if (childMode == ChildMode.PARALLEL)
            if (states.all { it.isFinished }) {
                _isFinished = true
                stateNotify { onFinished() }
            }
    }

    override fun doProcessEvent(event: Event, argument: Any?): Boolean {
        val machine = machine as InternalStateMachine

        if (isFinished) {
            machine.log("$this is finished, skipping event $event, with argument $argument")
            return false
        }

        val (transition, direction) = recursiveFindUniqueResolvedTransition(event) ?: return false

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

    override fun <E : Event> recursiveFindUniqueResolvedTransition(event: E): ResolvedTransition<E>? {
        val resolvedTransitions = getCurrentStates()
            .mapNotNull { it.recursiveFindUniqueResolvedTransition(event) }
            .ifEmpty { listOfNotNull(findUniqueResolvedTransition(event)) }
        check(resolvedTransitions.size <= 1) { "Multiple transitions match $event, $transitions in $this" }
        return resolvedTransitions.singleOrNull()
    }

    override fun recursiveEnterInitialStates() {
        if (states.isEmpty()) return

        when (childMode) {
            ChildMode.EXCLUSIVE -> {
                val initialState =
                    checkNotNull(initialState) { "Initial state is not set, call setInitialState() first" }
                setCurrentState(initialState, makeStartTransitionParams(initialState))
                initialState.recursiveEnterInitialStates()
            }
            ChildMode.PARALLEL -> _states.forEach {
                notifyStateEntry(it, makeStartTransitionParams(it))
                it.recursiveEnterInitialStates()
            }
        }
    }

    override fun recursiveEnterStatePath(path: MutableList<InternalState>, transitionParams: TransitionParams<*>) {
        if (path.isEmpty()) {
            recursiveEnterInitialStates()
        } else {
            val state = path.removeLast()
            setCurrentState(state, transitionParams)

            if (state !is StateMachine) // inner state machine manages its internal state by its own
                state.recursiveEnterStatePath(path, transitionParams)
        }
    }

    override fun recursiveExit(transitionParams: TransitionParams<*>) {
        for (currentState in getCurrentStates())
            currentState.recursiveExit(transitionParams)
        doExit(transitionParams)
    }

    override fun recursiveStop() {
        currentState = null
        _isActive = false
        _isFinished = false
        _states.forEach { it.recursiveStop() }
    }

    override fun recursiveFillActiveStates(states: MutableSet<IState>) {
        if (!_isActive) return
        states.add(this)

        for (currentState in getCurrentStates()) {
            // do not include nested state machine states
            if (currentState is StateMachine)
                states.add(currentState)
            else
                currentState.recursiveFillActiveStates(states)
        }
    }

    private fun requireCurrentState() = requireNotNull(currentState) { "Current state is not set" }

    private fun getCurrentStates() = when (childMode) {
        ChildMode.EXCLUSIVE -> listOfNotNull(currentState)
        ChildMode.PARALLEL -> _states.toList()
    }

    private fun setCurrentState(state: InternalState, transitionParams: TransitionParams<*>) {
        require(childMode == ChildMode.EXCLUSIVE) { "Cannot set current state in child mode $childMode" }
        require(states.contains(state)) { "$state is not a child of $this" }

        if (currentState == state) return
        currentState?.recursiveExit(transitionParams)
        currentState = state

        notifyStateEntry(state, transitionParams)
    }

    private fun notifyStateEntry(state: InternalState, transitionParams: TransitionParams<*>) {
        val finish = when (childMode) {
            ChildMode.EXCLUSIVE -> state is IFinalState
            ChildMode.PARALLEL -> states.all { it.isFinished }
        }

        if (finish) _isFinished = true

        state.doEnter(transitionParams)

        val machine = machine as InternalStateMachine
        if (finish) {
            machine.log("Parent $this finish")
            stateNotify { onFinished() }
        }

        machine.machineNotify { onStateChanged(state) }

        if (finish) parent?.afterChildFinished(this)
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

open class DefaultFinalState(name: String?) : DefaultState(name), FinalState {
    override fun <E : Event> addTransition(transition: Transition<E>) = super<FinalState>.addTransition(transition)
}

open class DefaultFinalDataState<out D>(name: String? = null) : DefaultDataState<D>(name), FinalDataState<D> {
    override fun <E : Event> addTransition(transition: Transition<E>) = super<FinalDataState>.addTransition(transition)
}
