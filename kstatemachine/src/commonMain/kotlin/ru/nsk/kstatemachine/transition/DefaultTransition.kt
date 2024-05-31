/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.transition

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.event.EventMatcher
import ru.nsk.kstatemachine.metainfo.MetaInfo
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.state.InternalState
import ru.nsk.kstatemachine.statemachine.StateMachine

open class DefaultTransition<E : Event>(
    override val name: String?,
    override val eventMatcher: EventMatcher<E>,
    override val type: TransitionType,
    sourceState: IState,
    override val metaInfo: MetaInfo?,
) : InternalTransition<E> {
    constructor(
        name: String?,
        eventMatcher: EventMatcher<E>,
        type: TransitionType,
        metaInfo: MetaInfo?,
        sourceState: IState,
        targetState: IState?
    ) : this(name, eventMatcher, type, sourceState, metaInfo) {
        targetStateDirectionProducer = { it.targetStateOrStay(targetState) }
    }

    constructor(
        name: String?,
        eventMatcher: EventMatcher<E>,
        type: TransitionType,
        metaInfo: MetaInfo?,
        sourceState: IState,
        targetStateDirectionProducer: TransitionDirectionProducer<E>
    ) : this(name, eventMatcher, type, sourceState, metaInfo) {
        this.targetStateDirectionProducer = targetStateDirectionProducer
    }

    private val _listeners = mutableSetOf<Transition.Listener>()
    override val listeners: Collection<Transition.Listener> get() = _listeners

    override val sourceState = sourceState as InternalState

    /**
     * Function that is called during event processing,
     * not during state machine configuration. So it is possible to check some outer (business logic) values in it.
     * If [Transition] does not have target state then [StateMachine] keeps current state
     * when such [Transition] is triggered.
     * This function should not have side effects.
     */
    private var targetStateDirectionProducer: TransitionDirectionProducer<E> = { stay() }

    override var argument: Any? = null

    override fun <L : Transition.Listener> addListener(listener: L): L {
        require(_listeners.add(listener)) { "$listener is already added" }
        return listener
    }

    override fun removeListener(listener: Transition.Listener) {
        _listeners.remove(listener)
    }

    override suspend fun isMatchingEvent(event: Event) = eventMatcher.match(event)

    override suspend fun produceTargetStateDirection(policy: TransitionDirectionProducerPolicy<E>) =
        targetStateDirectionProducer(policy)

    override fun toString() = "${this::class.simpleName}${if (name != null) "($name)" else ""}"
}