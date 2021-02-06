package ru.nsk.kstatemachine

import java.util.concurrent.CopyOnWriteArraySet

open class DefaultTransition<E : Event>(
    override val name: String?,
    override val eventMatcher: EventMatcher<E>,
    override val sourceState: State
) : InternalTransition<E> {
    private val _listeners = CopyOnWriteArraySet<Transition.Listener>()
    private val listeners: Set<Transition.Listener> get() = _listeners

    /**
     * Function that is called during event processing,
     * not during state machine configuration. So it is possible to check some outer (business logic) values in it.
     * If [Transition] does not have target state then [StateMachine] keeps current state
     * when such [Transition] is triggered.
     */
    private var targetStateDirectionProducer: () -> TransitionDirection = { stay() }

    override var argument: Any? = null

    constructor(
        name: String?,
        eventMatcher: EventMatcher<E>,
        sourceState: State,
        targetState: State?
    ) : this(name, eventMatcher, sourceState) {
        targetStateDirectionProducer = if (targetState == null) {
            { stay() }
        } else {
            { targetState(targetState) }
        }
    }

    constructor(
        name: String?,
        eventMatcher: EventMatcher<E>,
        sourceState: State,
        targetStateDirectionProducer: () -> TransitionDirection
    ) : this(name, eventMatcher, sourceState) {
        this.targetStateDirectionProducer = targetStateDirectionProducer
    }

    override fun <L : Transition.Listener> addListener(listener: L): L {
        require(_listeners.add(listener)) { "$listener is already added" }
        return listener
    }

    override fun removeListener(listener: Transition.Listener) {
        _listeners.remove(listener)
    }

    override fun isTriggeringEvent(event: Event) = eventMatcher.match(event)

    override fun produceTargetStateDirection() = targetStateDirectionProducer()

    override fun notify(block: Transition.Listener.() -> Unit) = listeners.forEach { it.apply(block) }

    override fun toString() = "${this::class.simpleName}(name=$name)"
}