package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.TreeAlgorithms.findPathFromTargetToLca
import ru.nsk.kstatemachine.visitors.GetActiveStatesVisitor
import java.util.concurrent.CopyOnWriteArraySet

open class BaseStateImpl(override val name: String?, override val childMode: ChildMode) : InternalState() {
    private val _listeners = CopyOnWriteArraySet<IState.Listener>()
    override val listeners: Collection<IState.Listener> get() = _listeners

    private val _states = mutableSetOf<InternalState>()
    override val states: Set<IState> get() = _states

    /**
     * In [ChildMode.EXCLUSIVE] might be null only before [setInitialState] call if there are child states.
     */
    private var currentState: InternalState? = null

    private var _initialState: InternalState? = null
    override val initialState get() = _initialState

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
        state.internalParent = this
        if (init != null) state.init()
        return state
    }

    override fun setInitialState(state: IState) {
        require(states.contains(state)) { "$state is not part of $this machine, use addState() first" }
        check(childMode == ChildMode.EXCLUSIVE) { "Can not set initial state in parallel child mode" }
        check(!machine.isRunning) { "Can not change initial state after state machine started" }

        _initialState = state as InternalState
    }

    override fun activeStates(selfIncluding: Boolean) = with(GetActiveStatesVisitor(selfIncluding)) {
        accept(this)
        return@with activeStates
    }

    override fun <E : Event> addTransition(transition: Transition<E>): Transition<E> {
        _transitions += transition
        return transition
    }

    override fun toString() = "${this::class.simpleName}${if (name != null) "($name)" else ""}"

    override fun asState() = this

    protected open fun onDoEnter(transitionParams: TransitionParams<*>) {
        /* default empty */
    }

    protected open fun onDoExit(transitionParams: TransitionParams<*>) {
        /* default empty */
    }

    override fun doEnter(transitionParams: TransitionParams<*>) {
        if (!_isActive) {
            if (parent != null) machine.log { "Parent $parent entering child $this" }
            _isActive = true
            onDoEnter(transitionParams)
            stateNotify { onEntry(transitionParams) }
        }
    }

    override fun doExit(transitionParams: TransitionParams<*>) {
        if (_isActive) {
            machine.log { "Exiting $this" }
            onDoExit(transitionParams)
            _isActive = false
            stateNotify { onExit(transitionParams) }
        }
    }

    override fun afterChildFinished(finishedChild: InternalState, transitionParams: TransitionParams<*>) {
        if (childMode == ChildMode.PARALLEL && states.all { it.isFinished }) {
            _isFinished = true
            machine.log { "$this finishes" }
            stateNotify { onFinished(transitionParams) }
        }
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

    private fun requireCurrentState() = requireNotNull(currentState) { "Current state is not set" }

    override fun getCurrentStates() = when (childMode) {
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

        if (finish) {
            _isFinished = true
            machine.log { "$this finishes" }
        }

        state.doEnter(transitionParams)

        val machine = machine as InternalStateMachine
        if (finish) stateNotify { onFinished(transitionParams) }

        machine.machineNotify { onStateChanged(state) }

        if (finish) internalParent?.afterChildFinished(this, transitionParams)
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
            transition.produceTargetStateDirection(TransitionDirectionProducerPolicy.DefaultPolicy(StartEvent)),
            StartEvent,
        )
    }
}