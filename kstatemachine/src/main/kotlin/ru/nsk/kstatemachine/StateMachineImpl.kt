package ru.nsk.kstatemachine

internal class StateMachineImpl(name: String?, childMode: ChildMode) :
    InternalStateMachine, DefaultState(name, childMode) {
    /** Access to this field must be thread safe. */
    private val _machineListeners = mutableSetOf<StateMachine.Listener>()
    override val machineListeners: Collection<StateMachine.Listener> get() = _machineListeners
    override var logger: StateMachine.Logger = NullLogger
    override var ignoredEventHandler = StateMachine.IgnoredEventHandler { _, _ -> }
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

    private var _isRunning = false
    override val isRunning get() = _isRunning

    private object NullLogger : StateMachine.Logger {
        override fun log(message: String) {}
    }

    @Synchronized
    override fun <L : StateMachine.Listener> addListener(listener: L): L {
        require(_machineListeners.add(listener)) { "$listener is already added" }

        val currentState = currentState
        if (currentState != null)
            listener.onStateChanged(currentState)
        return listener
    }

    @Synchronized
    override fun removeListener(listener: StateMachine.Listener) {
        _machineListeners.remove(listener)
    }

    override fun start() {
        run(makeStartTransitionParams(this))
        recursiveEnterInitialStates()
    }

    override fun startFrom(state: IState) {
        val transitionParams = makeStartTransitionParams(this, state)
        run(transitionParams)
        switchToTargetState(state as InternalState, this, transitionParams)
    }

    private fun run(transitionParams: TransitionParams<*>) {
        check(!isRunning) { "$this is already started" }
        if (childMode == ChildMode.EXCLUSIVE)
            checkNotNull(initialState) { "Initial state is not set, call setInitialState() first" }

        _isRunning = true
        log { "$this started" }
        machineNotify { onStarted() }
        doEnter(transitionParams)
    }

    override fun stop() {
        _isRunning = false
        recursiveStop()
        log { "$this stopped" }
        machineNotify { onStopped() }
    }

    @Synchronized
    override fun processEvent(event: Event, argument: Any?) {
        check(isRunning) { "$this is not started, call start() first" }

        if (isProcessingEvent)
            pendingEventHandler.onPendingEvent(event, argument)
        isProcessingEvent = true

        try {
            if (!doProcessEvent(event, argument)) {
                log { "$this ignored $event" }
                ignoredEventHandler.onIgnoredEvent(event, argument)
            }
        } finally {
            isProcessingEvent = false
        }
    }

    private fun doProcessEvent(event: Event, argument: Any?): Boolean {
        if (isFinished) {
            log { "$this is finished, skipping event $event, with argument $argument" }
            return false
        }

        val (transition, direction) = recursiveFindUniqueResolvedTransition(event) ?: return false

        val transitionParams = TransitionParams(transition, direction, event, argument)

        val targetState = direction.targetState as? InternalState

        val targetText = if (targetState != null) "to $targetState" else "[targetless]"
        log { "${event::class.simpleName} triggers $transition from ${transition.sourceState} $targetText" }

        transition.transitionNotify { onTriggered(transitionParams) }

        machineNotify { onTransition(transition.sourceState, targetState, event, argument) }

        targetState?.let { switchToTargetState(it, transition.sourceState, transitionParams) }
        return true
    }

    override fun activeStates(): Set<IState> {
        return mutableSetOf<IState>().also { recursiveFillActiveStates(it) }
    }

    override fun log(lazyMessage: () -> String) {
        if (logger != NullLogger)
            logger.log(lazyMessage())
    }

    /**
     *  Starts machine if its inner state machine
     */
    override fun doEnter(transitionParams: TransitionParams<*>) =
        if (!isRunning) start() else super.doEnter(transitionParams)
}
