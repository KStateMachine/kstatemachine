package ru.nsk.kstatemachine

import java.util.concurrent.CopyOnWriteArraySet

/**
 * Simple finite state machine (FSM) implementation.
 */
class StateMachine(val name: String?, private val logger: Logger?) {
    private val states = mutableSetOf<State>()
    private lateinit var currentState: State
    private val listeners = CopyOnWriteArraySet<Listener>()

    fun <S: State> addState(state: S): S {
        states += state
        return state
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    /**
     * Now initial state is mandatory, but if we add parallel states it will not be mandatory
     */
    fun setInitialState(state: State) {
        require(states.contains(state)) { "$state is not part of $this machine, use addState() first" }
        currentState = state.also { it.addTransition(StartTransition) }
    }

    fun log(message: String) = logger?.log(message)

    fun processEvent(event: Event, argument: Any? = null) {
        val fromState = currentState
        val transition = fromState.findTransition(event)

        if (transition != null) {
            val transitionParams = TransitionParams(transition, event, argument)

            log("$this triggering $transition from $fromState")
            transition.notify { onTriggered(transitionParams) }

            val targetState = transition.produceTargetState()
            notify { onTransition(transition.sourceState, targetState, event, argument) }

            if (event is StartEvent) {
                log("$this entering $fromState")
                fromState.notify { onEntry(transitionParams) }
            }

            targetState?.let { _ ->
                log("$this exiting $fromState")
                fromState.notify { onExit(transitionParams) }

                currentState = targetState
                log("$this entering $targetState")
                targetState.notify { onEntry(transitionParams) }
            }
        } else {
            log("$this skipping $event as transition from $fromState, was not found")
        }
    }

    fun start() {
        processEvent(StartEvent)
    }

    override fun toString() = "${javaClass.simpleName}(name=$name)"

    private fun <E : Event> State.findTransition(event: E): Transition<E>? {
        val triggeringTransitions = transitions.filter { it.isTriggeringEvent(event) }
        check(triggeringTransitions.size <= 1) { "Multiple transitions match $event $triggeringTransitions in $this" }
        return triggeringTransitions.firstOrNull() as Transition<E>?
    }

    private fun notify(block: Listener.() -> Unit) = listeners.forEach { it.apply(block) }

    /**
     * State machine uses this interface to support logging on different platforms
     */
    fun interface Logger {
        fun log(message: String)
    }

    interface Listener {
        /**
         * This method is called when transition is performed.
         * There might be may transitions from one state to another,
         * this method might be used to listen to all transitions in one place
         * instead of listening for each transition separately.
         */
        fun onTransition(sourceState: State, targetState: State?, event: Event, argument: Any?) {}
    }

    private object StartEvent : Event
    private object StartTransition : Transition<StartEvent>(StartEvent::class.java, StartState, "Starting")
    private object StartState : State("Start")
}

fun StateMachine.onTransition(block: (sourceState: State, targetState: State?, event: Event, argument: Any?) -> Unit) {
    addListener(object : StateMachine.Listener {
        override fun onTransition(sourceState: State, targetState: State?, event: Event, argument: Any?) =
            block(sourceState, targetState, event, argument)
    })
}

fun StateMachine.state(name: String, init: (State.() -> Unit)? = null): State {
    val state = State(name)
    if (init != null) state.init()
    return addState(state)
}

/**
 * Factory method for creating [StateMachine]
 * @param logger state machine will use this function to log its internal state. It may be used in debugging purpose.
 */
fun createStateMachine(
    name: String? = null,
    logger: StateMachine.Logger? = null,
    init: StateMachine.() -> Unit
) = StateMachine(name, logger).apply {
    init()
    start()
}

data class TransitionParams<E : Event>(
    val transition: Transition<E>,
    val event: E,
    /**
     * This parameter may be used to pass arbitrary data with the event,
     * so there is no need to define [Event] subclasses every time.
     * Subclassing should be preferred if the event always contains data of some type.
     */
    val argument: Any? = null
)