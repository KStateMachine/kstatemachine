package ru.nsk.kstatemachine

internal class StateMachineImpl(name: String?) : InternalStateMachine, DefaultState(name) {
    /** Access to this field must be thread safe. */
    private val listeners = mutableSetOf<StateMachine.Listener>()
    override var logger = StateMachine.Logger {}
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

    override fun start() {
        check(!isRunning) { "$this is already started" }
        checkNotNull(initialState) { "Initial state is not set, call setInitialState() first" }

        _isRunning = true
        machineNotify { onStarted() }
        doEnter(makeStartTransitionParams(this))

        recursiveEnterInitialState()
    }

    override fun stop() {
        _isRunning = false
        machineNotify { onStopped() }
    }

    @Synchronized
    override fun processEvent(event: Event, argument: Any?) {
        check(isRunning) { "$this is not started, call start() first" }

        if (isProcessingEvent)
            pendingEventHandler.onPendingEvent(event, argument)
        isProcessingEvent = true

        try {
            if (!recursiveProcessEvent(event, argument)) {
                log("$this ignored $event")
                ignoredEventHandler.onIgnoredEvent(event, argument)
            }
        } finally {
            isProcessingEvent = false
        }
    }

    /**
     *  Starts machine if its inner state machine
     */
    override fun doEnter(transitionParams: TransitionParams<*>) =
        if (!isRunning) start() else super.doEnter(transitionParams)

    override fun machineNotify(block: StateMachine.Listener.() -> Unit) = listeners.forEach { it.apply(block) }

    override fun toString() = "${this::class.simpleName}(name=$name)"
}
