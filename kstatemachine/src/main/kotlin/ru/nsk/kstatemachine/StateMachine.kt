package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.visitors.Visitor

@DslMarker
annotation class StateMachineDslMarker

interface StateMachine : State {
    var logger: Logger
    var ignoredEventHandler: IgnoredEventHandler
    var pendingEventHandler: PendingEventHandler
    val isRunning: Boolean
    val machineListeners: Collection<Listener>

    fun <L : Listener> addListener(listener: L): L
    fun removeListener(listener: Listener)

    /**
     * Starts state machine
     */
    fun start()

    /**
     * Forces state machine to stop
     */
    fun stop()

    /**
     * Machine must be started to process events
     */
    fun processEvent(event: Event, argument: Any? = null)

    fun log(message: String) = logger.log(message)

    override fun accept(visitor: Visitor) = visitor.visit(this)

    interface Listener {
        /**
         * Notifies that state machine started (entered initial state).
         */
        fun onStarted() = Unit

        /**
         * This method is called when transition is performed.
         * There might be may transitions from one state to another,
         * this method might be used to listen to all transitions in one place
         * instead of listening for each transition separately.
         */
        fun onTransition(sourceState: State, targetState: State?, event: Event, argument: Any?) = Unit

        /**
         * Notifies about state changes.
         * This method will also be triggered on adding listener with a current state of a state machine.
         */
        fun onStateChanged(newState: State) = Unit

        /**
         * Notifies that state machine has stopped.
         */
        fun onStopped() = Unit
    }

    /**
     * State machine uses this interface to support internal logging on different platforms
     */
    fun interface Logger {
        fun log(message: String)
    }

    fun interface IgnoredEventHandler {
        fun onIgnoredEvent(event: Event, argument: Any?)
    }

    fun interface PendingEventHandler {
        fun onPendingEvent(pendingEvent: Event, argument: Any?)
    }
}

typealias StateMachineBlock = StateMachine.() -> Unit

fun StateMachine.onStarted(block: StateMachine.() -> Unit) {
    addListener(object : StateMachine.Listener {
        override fun onStarted() = block()
    })
}

fun StateMachine.onTransition(
    block: StateMachine.(
        sourceState: State,
        targetState: State?,
        event: Event,
        argument: Any?
    ) -> Unit
) {
    addListener(object : StateMachine.Listener {
        override fun onTransition(sourceState: State, targetState: State?, event: Event, argument: Any?) =
            block(sourceState, targetState, event, argument)
    })
}

fun StateMachine.onStateChanged(block: StateMachine.(newState: State) -> Unit) {
    addListener(object : StateMachine.Listener {
        override fun onStateChanged(newState: State) = block(newState)
    })
}

/**
 * Factory method for creating [StateMachine]
 */
fun createStateMachine(
    name: String? = null,
    start: Boolean = true,
    init: StateMachineBlock
): StateMachine = StateMachineImpl(name).apply {
    init()
    if (start) start()
}

/**
 * Defines state machine API for internal library usage.
 */
interface InternalStateMachine : StateMachine, InternalState

fun InternalStateMachine.machineNotify(block: StateMachine.Listener.() -> Unit) =
    machineListeners.forEach { it.apply(block) }