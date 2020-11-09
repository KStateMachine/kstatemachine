package ru.nsk.kstatemachine

import java.util.concurrent.CopyOnWriteArraySet

/**
 * Base interface for events which may trigger transitions of [StateMachine]
 */
interface Event

/**
 * Represent a transition between states, which gets triggered when specified [Event] is posted to [StateMachine]
 */
open class Transition<E : Event>(
    private val eventMatcher: EventMatcher,
    val sourceState: State,
    val name: String?
) {
    private val _listeners = CopyOnWriteArraySet<Listener>()
    private val listeners: Set<Listener> = _listeners

    /**
     * Function that is called during event processing,
     * not during state machine configuration. So it is possible to check some outer (business logic) values in it.
     * If [Transition] does not have target state then [StateMachine] keeps current state
     * when such [Transition] is triggered.
     */
    private var targetStateDirectionProducer: () -> TransitionDirection = { stay() }

    /**
     * This parameter may be used to pass arbitrary data with a transition to targetState.
     * This argument must be set from transition listener. Such transition must have only one listener setting argument.
     */
    var argument: Any? = null

    constructor(
        eventMatcher: EventMatcher,
        sourceState: State,
        targetState: State?,
        name: String?
    ) : this(eventMatcher, sourceState, name) {
        targetStateDirectionProducer = if (targetState == null) {
            { stay() }
        } else {
            { targetState(targetState) }
        }
    }

    constructor(
        eventMatcher: EventMatcher,
        sourceState: State,
        targetStateDirectionProducer: () -> TransitionDirection,
        name: String?
    ) : this(eventMatcher, sourceState, name) {
        this.targetStateDirectionProducer = targetStateDirectionProducer
    }

    internal fun produceTargetStateDirection() = targetStateDirectionProducer()

    fun <L : Listener> addListener(listener: L): L {
        require(_listeners.add(listener)) { "$listener is already added" }
        return listener
    }

    fun removeListener(listener: Listener) {
        _listeners.remove(listener)
    }

    /**
     * Check if event can trigger this [Transition]
     */
    open fun isTriggeringEvent(event: Event): Boolean {
        return eventMatcher.match(event)
    }

    internal fun notify(block: Listener.() -> Unit) = listeners.forEach { it.apply(block) }

    override fun toString() = "${javaClass.simpleName}(name=$name)"

    interface Listener {
        fun onTriggered(transitionParams: TransitionParams<*>) {}
    }
}

inline fun <reified E : Event> Transition<E>.onTriggered(crossinline block: (TransitionParams<E>) -> Unit) {
    addListener(object : Transition.Listener {
        override fun onTriggered(transitionParams: TransitionParams<*>) = block(transitionParams as TransitionParams<E>)
    })
}
