package ru.nsk.kstatemachine

import java.util.concurrent.CopyOnWriteArraySet

open class DefaultState(override val name: String? = null) : InternalState {
    private val _listeners = CopyOnWriteArraySet<State.Listener>()

    private val _states = mutableSetOf<InternalState>()
    override val states: Set<State> = _states

    /**
     * Might be null only before [setInitialState] call.
     */
    protected var currentState: InternalState? = null

    private var _initialState: InternalState? = null
    override val initialState
        get() = _initialState

    private var _parent: State? = null
    override val parent: State
        get() = requireNotNull(_parent) { "Parent state not set, call setParent() first" }

    override val machine: StateMachine
        get() = if (this is StateMachine) this else parent.machine

    private val _transitions = mutableSetOf<Transition<*>>()
    override val transitions: Set<Transition<*>> = _transitions

    private var isFinished = false

    override fun <E : Event> addTransition(transition: Transition<E>): Transition<E> {
        _transitions += transition
        return transition
    }

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

        require(_states.add(state as InternalState)) { "$state already added" }
        (state as InternalState).setParent(this)
        if (init != null) state.init()
        return state
    }

    override fun findState(name: String) = states.find { it.name == name }
    override fun requireState(name: String) = findState(name) ?: throw IllegalArgumentException("State $name not found")

    override fun setInitialState(state: State) {
        require(states.contains(state)) { "$state is not part of $this machine, use addState() first" }
        check(!machine.isRunning) { "Can not change initial state after state machine started" }

        _initialState = state as InternalState
    }

    override fun setParent(parent: State) {
        _parent = parent
    }

    /**
     * Get transition by name. This might be used to start listening to transition after state machine setup.
     */

    /**
     * Get transition by name. This might be used to start listening to transition after state machine setup.
     */
    override fun findTransition(name: String) = transitions.find { it.name == name }
    override fun requireTransition(name: String) =
        findTransition(name) ?: throw IllegalArgumentException("Transition $name not found")

    override fun notify(block: State.Listener.() -> Unit) = _listeners.forEach { it.apply(block) }

    override fun <E : Event> findTransitionByEvent(event: E): InternalTransition<E>? {
        val triggeringTransitions = transitions.filter { it.isTriggeringEvent(event) }
        check(triggeringTransitions.size <= 1) { "Multiple transitions match $event $triggeringTransitions in $this" }
        @Suppress("UNCHECKED_CAST")
        return triggeringTransitions.firstOrNull() as InternalTransition<E>?
    }

    override fun toString() = "${this::class.simpleName}(name=$name)"

    override fun asState() = this

    override fun doEnter() {
        if (states.isEmpty()) return

        val initialState = checkNotNull(initialState) { "Initial state is not set, call setInitialState() first" }

        setCurrentState(
            initialState,
            TransitionParams(
                DefaultTransition(
                    EventMatcher.isInstanceOf(),
                    initialState,
                    initialState,
                    "Starting"
                ), StartEvent
            )
        )
    }

    override fun doExit(transitionParams: TransitionParams<*>) {
        if (states.isNotEmpty()) {
            val currentState = requireNotNull(currentState) { "currentState is not set" }
            currentState.doExit(transitionParams)
        }

        machine.log("Exiting $this")
        notify { onExit(transitionParams) }
    }

    override fun doProcessEvent(event: Event, argument: Any?): Boolean {
        val machine = machine as InternalStateMachine

        if (isFinished)
            machine.log("$this is finished, skipping event $event, with argument $argument")

        val fromState = if (currentState != null) currentState!! else return false
        val transition = fromState.findTransitionByEvent(event)

        if (transition != null) {
            val transitionParams = TransitionParams(transition, event, argument)

            val direction = transition.produceTargetStateDirection()
            val targetState = if (direction is TargetState) direction.targetState as InternalState else null

            if (direction !is NoTransition) {
                machine.log("$this triggering $transition from $fromState")
                transition.notify { onTriggered(transitionParams) }

                machine.machineNotify { onTransition(transition.sourceState, targetState, event, argument) }
            }

            targetState?.let { switchToTargetState(it, fromState, transitionParams) }
            return true
        } else {
            return fromState.doProcessEvent(event, argument)
        }
    }

    private fun setCurrentState(state: InternalState, transitionParams: TransitionParams<*>) {
        val machine = machine as InternalStateMachine
        require(states.contains(state)) { "$state is not a child of $this" }

        currentState = state

        val finish = state is FinalState
        if (finish) isFinished = true

        machine.log("Parent $this entering child $state")

        state.notify { onEntry(transitionParams) }

        if (finish) {
            machine.log("Parent $this finish")
            notify { onFinished() }
        }

        machine.machineNotify { onStateChanged(state) }

        state.doEnter()
    }

    private fun switchToTargetState(
        targetState: InternalState,
        fromState: InternalState,
        transitionParams: TransitionParams<*>
    ) {
        val lca = findLowestCommonAncestor(this, targetState)

        fromState.doExit(transitionParams)
        val list = findTargetListTo(targetState)
        setCurrentState(targetState, transitionParams)
    }



    private fun findTargetListTo(state: InternalState): List<InternalState> {
        val list = mutableListOf<InternalState>()
        doFindTargetListTo(state, list)
        return list
    }

    override fun doFindTargetListTo(state: InternalState, list: MutableList<InternalState>) {
        if (states.contains(state)) {
            list.add(state)
        } else {
            _states.forEach {
                it.doFindTargetListTo(state, list)
            }
        }
    }

    /**
     * Initial event which is processed on state machine start
     */
    private object StartEvent : Event

    private companion object {
        private fun findLowestCommonAncestor(first: InternalState, second: InternalState) : InternalState {
            return first
        }
    }
}

open class DefaultFinalState(name: String? = null) : DefaultState(name), FinalState {
    override fun <E : Event> addTransition(transition: Transition<E>) =
        throw UnsupportedOperationException("FinalState can not have transitions")
}