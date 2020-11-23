package ru.nsk.kstatemachine

import java.util.concurrent.CopyOnWriteArraySet

open class DefaultTransition<E : Event>(
    override val eventMatcher: EventMatcher<E>,
    override val sourceState: State,
    override val name: String?
) : InternalTransition<E> {
    private val _listeners = CopyOnWriteArraySet<Transition.Listener>()
    private val listeners: Set<Transition.Listener> = _listeners

    /**
     * Function that is called during event processing,
     * not during state machine configuration. So it is possible to check some outer (business logic) values in it.
     * If [Transition] does not have target state then [StateMachine] keeps current state
     * when such [Transition] is triggered.
     */
    private var targetStateDirectionProducer: () -> TransitionDirection = { stay() }

    override var argument: Any? = null

    constructor(
        eventMatcher: EventMatcher<E>,
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
        eventMatcher: EventMatcher<E>,
        sourceState: State,
        targetStateDirectionProducer: () -> TransitionDirection,
        name: String?
    ) : this(eventMatcher, sourceState, name) {
        this.targetStateDirectionProducer = targetStateDirectionProducer
    }

    override fun <L : Transition.Listener> addListener(listener: L): L {
        require(_listeners.add(listener)) { "$listener is already added" }
        return listener
    }

    override fun removeListener(listener: Transition.Listener) {
        _listeners.remove(listener)
    }

    override fun isTriggeringEvent(event: Event): Boolean {
        return eventMatcher.match(event)
    }

    override fun produceTargetStateDirection() = targetStateDirectionProducer()

    override fun notify(block: Transition.Listener.() -> Unit) = listeners.forEach { it.apply(block) }

    override fun toString() = "${javaClass.simpleName}(name=$name)"
}