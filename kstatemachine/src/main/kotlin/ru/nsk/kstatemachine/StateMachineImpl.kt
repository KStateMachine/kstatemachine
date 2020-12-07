package ru.nsk.kstatemachine

internal class StateMachineImpl(override val name: String?) : StateMachine {
    private val _states = mutableSetOf<State>()
    override val states: Set<State> = _states

    /**
     * Might be null only before [setInitialState] call.
     * Access to this field must be thread safe.
     */
    private var currentState: InternalState? = null

    /** Access to this field must be thread safe. */
    private val listeners = mutableSetOf<StateMachine.Listener>()
    override var logger = StateMachine.Logger {}
    override var ignoredEventHandler = StateMachine.IgnoredEventHandler { _, _, _ -> }
    override var pendingEventHandler = StateMachine.PendingEventHandler { pendingEvent, _ ->
        error(
            "$this can not process pending $pendingEvent as event processing is already running. " +
                    "Do not call processEvent() from notification listeners."
        )
    }

    /**
     * Help to check that [processEvent] is not called from state machine notification method.
     * Access to this field must be thread safe.
     */
    private var isProcessingEvent = false
    private var isStarted = false
    private var isFinished = false

    @Synchronized
    override fun <L : StateMachine.Listener> addListener(listener: L): L {
        require(listeners.add(listener)) { "$listener is already added" }

        val currentState = currentState
        if (currentState != null)
            listener.onStateChanged(currentState)
        return listener
    }

    @Synchronized
    override fun removeListener(listener: StateMachine.Listener) {
        listeners.remove(listener)
    }

    override fun <S : State> addState(state: S, init: StateBlock?): S {
        val name = state.name
        if (name != null)
            require(findState(name) == null) { "State with name $name already exists" }

        if (init != null) state.init()
        _states += state
        return state
    }

    override fun <S : State> addInitialState(state: S, init: StateBlock?): S {
        addState(state, init)
        setInitialState(state)
        return state
    }

    override fun findState(name: String) = states.find { it.name == name }
    override fun requireState(name: String) = findState(name) ?: throw IllegalArgumentException("State $name not found")

    /**
     * Now initial state is mandatory, but if we add parallel states it will not be mandatory.
     */
    override fun setInitialState(state: State) {
        require(states.contains(state)) { "$state is not part of $this machine, use addState() first" }
        currentState = state as InternalState
    }

    @Synchronized
    override fun processEvent(event: Event, argument: Any?) {
        check(isStarted) { "$this is not started, call start() first" }
        if (isFinished) log("$this is finished, ignoring event $event, with argument $argument")

        if (isProcessingEvent)
            pendingEventHandler.onPendingEvent(event, argument)
        isProcessingEvent = true

        try {
            val fromState = currentState!!
            val transition = fromState.findTransitionByEvent(event)

            if (transition != null) {
                val transitionParams = TransitionParams(transition, event, argument)

                val direction = transition.produceTargetStateDirection()
                val targetState = if (direction is TargetState) direction.targetState as InternalState else null

                if (direction !is NoTransition) {
                    log("$this triggering $transition from $fromState")
                    transition.notify { onTriggered(transitionParams) }

                    notify { onTransition(transition.sourceState, targetState, event, argument) }
                }

                targetState?.let { _ ->
                    log("$this exiting $fromState")
                    fromState.notify { onExit(transitionParams) }

                    setCurrentState(targetState, transitionParams)
                }
            } else {
                log("$this ignored $event as transition from $fromState, was not found")
                ignoredEventHandler.onIgnoredEvent(fromState, event, argument)
            }
        } finally {
            isProcessingEvent = false
        }
    }

    private fun log(message: String) = logger.log(message)

    private fun setCurrentState(state: InternalState, transitionParams: TransitionParams<*>) {
        currentState = state

        val finish = state is FinalState
        if (finish) isFinished = true

        log("$this entering $state")

        state.notify {
            onEntry(transitionParams)
            if (finish) onExit(transitionParams)
        }

        notify {
            onStateChanged(state)
            if (finish) onFinished()
        }
    }

    internal fun start() {
        val currentState = checkNotNull(currentState) { "Initial state is not set, call setInitialState() first" }

        isStarted = true
        notify { onStarted() }

        setCurrentState(
            currentState,
            TransitionParams(
                DefaultTransition(
                    EventMatcher.isInstanceOf(),
                    currentState,
                    currentState,
                    "Starting"
                ), StartEvent
            )
        )
    }

    override fun toString() = "${this::class.simpleName}(name=$name)"

    private fun notify(block: StateMachine.Listener.() -> Unit) = listeners.forEach { it.apply(block) }

    /**
     * Initial event which is processed on state machine start
     */
    private object StartEvent : Event
}
