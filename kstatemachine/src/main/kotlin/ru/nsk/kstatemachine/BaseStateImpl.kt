package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.TransitionDirectionProducerPolicy.DefaultPolicy
import ru.nsk.kstatemachine.TreeAlgorithms.findPathFromTargetToLca
import ru.nsk.kstatemachine.visitors.GetActiveStatesVisitor

open class BaseStateImpl(override val name: String?, override val childMode: ChildMode) : InternalState() {

    private class Data {
        val listeners = mutableSetOf<IState.Listener>()
        val states = mutableSetOf<InternalState>()
        var initialState: InternalState? = null

        /**
         * In [ChildMode.EXCLUSIVE] is null if there are no child states, or if a state is not active.
         * In [ChildMode.PARALLEL] it is always null.
         */
        var currentState: InternalState? = null
        val transitions = mutableSetOf<Transition<*>>()
        var isActive = false
        var isFinished = false
        var internalParent: InternalState? = null
    }

    /**
     * Encapsulates all mutable fields
     */
    private var data = Data()

    override val listeners: Collection<IState.Listener> get() = data.listeners

    override val states: Set<IState> get() = data.states

    override val initialState get() = data.initialState

    private val historyStates: List<HistoryState> get() = states.filterIsInstance<HistoryState>()

    override val machine get() = if (this is StateMachine) this else requireParent().machine

    override val transitions: Set<Transition<*>> get() = data.transitions

    override val isActive get() = data.isActive
    override val isFinished get() = data.isFinished


    override val internalParent get() = data.internalParent

    override fun setParent(parent: InternalState) {
        check(parent !== data.internalParent) { "$parent is already a parent of $this" }
        if (data.internalParent != null)
            onStateReuseDetected()
        data.internalParent = parent
    }

    private fun onStateReuseDetected() {
        if (machine.autoDestroyOnStatesReuse)
            machine.destroy()
        else
            error("State $this is already used in another machine instance")
    }

    override fun <L : IState.Listener> addListener(listener: L): L {
        require(data.listeners.add(listener)) { "$listener is already added" }
        return listener
    }

    override fun removeListener(listener: IState.Listener) {
        data.listeners.remove(listener)
    }

    override fun <S : IState> addState(state: S, init: StateBlock<S>?): S {
        check(!machine.isRunning) { "Can not add state after state machine started" }
        if (childMode == ChildMode.PARALLEL) {
            require(state !is FinalState) { "Can not add FinalState in parallel child mode" }
            if (state is HistoryState)
                require(state.historyType != HistoryType.SHALLOW) {
                    "Can not add Shallow HistoryState in parallel child mode"
                }
        }

        state.name?.let {
            require(findState(it, recursive = false) == null) { "State with name $it already exists" }
        }

        state as InternalState
        require(data.states.add(state)) { "$state already added" }
        state.setParent(this)

        if (init != null) state.init()
        return state
    }

    override fun setInitialState(state: IState) {
        require(states.contains(state)) { "$state is not part of $this machine, use addState() first" }
        check(childMode == ChildMode.EXCLUSIVE) { "Can not set initial state in parallel child mode" }
        check(!machine.isRunning) { "Can not change initial state after state machine started" }

        data.initialState = state as InternalState
    }

    override fun activeStates(selfIncluding: Boolean) = with(GetActiveStatesVisitor(selfIncluding)) {
        accept(this)
        return@with activeStates
    }

    override fun <E : Event> addTransition(transition: Transition<E>): Transition<E> {
        data.transitions += transition
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
        if (!isActive) {
            if (parent != null) machine.log { "Parent $parent entering child $this" }
            data.isActive = true
            onDoEnter(transitionParams)
            stateNotify { onEntry(transitionParams) }
        }
    }

    override fun doExit(transitionParams: TransitionParams<*>) {
        if (isActive) {
            machine.log { "Exiting $this" }
            onDoExit(transitionParams)
            data.currentState = null
            data.isActive = false
            stateNotify { onExit(transitionParams) }
        }
    }

    override fun afterChildFinished(finishedChild: InternalState, transitionParams: TransitionParams<*>) {
        if (childMode == ChildMode.PARALLEL && states.all { it.isFinished }) {
            data.isFinished = true
            machine.log { "$this finishes" }
            stateNotify { onFinished(transitionParams) }
        }
    }

    override fun <E : Event> recursiveFindUniqueResolvedTransition(
        eventAndArgument: EventAndArgument<E>
    ): ResolvedTransition<E>? {
        val resolvedTransitions = getCurrentStates()
            .mapNotNull { it.recursiveFindUniqueResolvedTransition(eventAndArgument) }
            .ifEmpty { listOfNotNull(findUniqueResolvedTransition(eventAndArgument)) }
        check(resolvedTransitions.size <= 1) {
            "Multiple transitions match ${eventAndArgument.event}, $transitions in $this"
        }
        return resolvedTransitions.singleOrNull()
    }

    override fun recursiveEnterInitialStates(argument: Any?) {
        if (states.isEmpty()) return

        when (childMode) {
            ChildMode.EXCLUSIVE -> {
                val initialState = checkNotNull(initialState) {
                    "Initial state is not set, call setInitialState() first"
                }
                setCurrentState(initialState, makeStartTransitionParams(initialState, argument = argument))
                initialState.recursiveEnterInitialStates(argument)
            }
            ChildMode.PARALLEL -> data.states.forEach {
                notifyStateEntry(it, makeStartTransitionParams(it, argument = argument))
                it.recursiveEnterInitialStates(argument)
            }
        }
    }

    override fun recursiveEnterStatePath(path: MutableList<InternalState>, transitionParams: TransitionParams<*>) {
        if (path.isEmpty()) {
            recursiveEnterInitialStates(transitionParams.argument)
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
        data.currentState = null
        data.isActive = false
        data.isFinished = false
        data.states.forEach { it.recursiveStop() }
    }

    private fun requireCurrentState() = requireNotNull(data.currentState) { "Current state is not set" }

    override fun getCurrentStates() = when (childMode) {
        ChildMode.EXCLUSIVE -> listOfNotNull(data.currentState)
        ChildMode.PARALLEL -> data.states.toList()
    }

    private fun setCurrentState(state: InternalState, transitionParams: TransitionParams<*>) {
        require(childMode == ChildMode.EXCLUSIVE) { "Cannot set current state in child mode $childMode" }
        require(states.contains(state)) { "$state is not a child of $this" }

        if (data.currentState == state) return

        // store history
        data.currentState?.let { currentState ->
            // FIXME only State is supported (not DataState), add some check?
            historyStates.forEach { it.storeState(this, currentState) }
        }

        data.currentState?.recursiveExit(transitionParams)
        data.currentState = state

        notifyStateEntry(state, transitionParams)
    }

    override fun cleanup() {
        data = Data()
        onCleanup()
    }

    private fun notifyStateEntry(state: InternalState, transitionParams: TransitionParams<*>) {
        val finish = when (childMode) {
            ChildMode.EXCLUSIVE -> state is IFinalState
            ChildMode.PARALLEL -> states.all { it.isFinished }
        }

        if (finish) {
            data.isFinished = true
            machine.log { "$this finishes" }
        }

        state.doEnter(transitionParams)

        val machine = machine as InternalStateMachine
        machine.machineNotify { onStateChanged(state) }

        if (finish) {
            stateNotify { onFinished(transitionParams) }
            internalParent?.afterChildFinished(this, transitionParams)
        }
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
        targetState: IState = sourceState,
        argument: Any?
    ): TransitionParams<*> {
        val transition = DefaultTransition(
            "Starting",
            EventMatcher.isInstanceOf<StartEvent>(),
            sourceState,
            targetState,
        )

        return TransitionParams(
            transition,
            transition.produceTargetStateDirection(DefaultPolicy(EventAndArgument(StartEvent, argument))),
            StartEvent,
            argument,
        )
    }
}
