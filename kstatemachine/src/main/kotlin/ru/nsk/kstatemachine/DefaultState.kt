package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.TreeAlgorithms.findPathFromTargetToLca
import java.util.concurrent.CopyOnWriteArraySet

open class DefaultState(override val name: String? = null) : InternalState {
    private val _listeners = CopyOnWriteArraySet<State.Listener>()
    override val listeners: Collection<State.Listener> get() = _listeners

    private val _states = mutableSetOf<InternalState>()
    override val states: Set<State> get() = _states

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

    override fun <L : State.Listener> addListener(listener: L): L {
        require(_listeners.add(listener)) { "$listener is already added" }
        return listener
    }

    override fun removeListener(listener: State.Listener) {
        _listeners.remove(listener)
    }

    override fun <S : State> addState(state: S, init: StateBlock<S>?): S {
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

    override fun findState(name: String) = states.find { it.name == name }

    override fun setInitialState(state: State) {
        require(states.contains(state)) { "$state is not part of $this machine, use addState() first" }
        check(!machine.isRunning) { "Can not change initial state after state machine started" }

        _initialState = state as InternalState
    }

    override fun <E : Event> addTransition(transition: Transition<E>): Transition<E> {
        _transitions += transition
        return transition
    }

    /**
     * Get transition by name. This might be used to start listening to transition after state machine setup.
     */
    override fun findTransition(name: String) = transitions.find { it.name == name }

    override fun toString() = "${this::class.simpleName}(name=$name)"

    override fun asState() = this

    override fun doEnter(transitionParams: TransitionParams<*>) {
        if (!_isActive) {
            machine.log("Parent $parent entering child $this")
            _isActive = true
            stateNotify { onEntry(transitionParams) }
        }
    }

    override fun doExit(transitionParams: TransitionParams<*>) {
        if (_isActive) {
            machine.log("Exiting $this")
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

    override fun recursiveFillActiveStates(states: MutableSet<State>) {
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

        val finish = state is FinalState
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

    internal fun makeStartTransitionParams(sourceState: State, targetState: State = sourceState): TransitionParams<*> {
        val transition = DefaultTransition(
            "Starting",
            EventMatcher.isInstanceOf(),
            sourceState,
            targetState,
        )

        return TransitionParams(
            transition,
            transition.produceTargetStateDirection(),
            StartEvent
        )
    }
}

open class DefaultArgState<A : Any>(override val name: String? = null) : DefaultState(name), ArgState<A> {
    private var _arg: A? = null
    override val arg: A get() = requireNotNull(_arg) { "Type safe argument is not set. Is the state active?" }

    override fun doEnter(transitionParams: TransitionParams<*>) {
        @Suppress("UNCHECKED_CAST")
        if (!isActive) _arg = (transitionParams.event as ArgEvent<A>).arg
        super.doEnter(transitionParams)
    }

    override fun doExit(transitionParams: TransitionParams<*>) {
        super.doExit(transitionParams)
        if (isActive) _arg = null
    }
}

open class DefaultFinalState(name: String? = null) : DefaultState(name), FinalState {
    override fun <E : Event> addTransition(transition: Transition<E>) =
        throw UnsupportedOperationException("FinalState can not have transitions")
}
