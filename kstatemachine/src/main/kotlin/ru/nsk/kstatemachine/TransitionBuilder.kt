package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.TransitionDirectionProducerPolicy.CollectTargetStatesPolicy
import ru.nsk.kstatemachine.TransitionDirectionProducerPolicy.DefaultPolicy

@StateMachineDslMarker
abstract class TransitionBuilder<E : Event>(protected val name: String?, protected val sourceState: IState) {
    var listener: Transition.Listener? = null
    lateinit var eventMatcher: EventMatcher<E>

    abstract fun build(): Transition<E>
}

abstract class BaseGuardedTransitionBuilder<E : Event>(name: String?, sourceState: IState) :
    TransitionBuilder<E>(name, sourceState) {
    var guard: EventAndArgument<E>.() -> Boolean = { true }
}

abstract class GuardedTransitionBuilder<E : Event, S : IState>(name: String?, sourceState: IState) :
    BaseGuardedTransitionBuilder<E>(name, sourceState) {
    var targetState: S? = null

    override fun build(): Transition<E> {
        val direction: TransitionDirectionProducer<E> = {
            when (it) {
                is DefaultPolicy<E> ->
                    if (it.eventAndArgument.guard())
                        it.targetStateOrStay(targetState)
                    else
                        noTransition()
                is CollectTargetStatesPolicy<E> -> it.targetStateOrStay(targetState)
            }
        }

        val transition = DefaultTransition(name, eventMatcher, sourceState, direction)
        listener?.let { transition.addListener(it) }
        return transition
    }
}

abstract class GuardedTransitionOnBuilder<E : Event, S : IState>(name: String?, sourceState: IState) :
    BaseGuardedTransitionBuilder<E>(name, sourceState) {
    lateinit var targetState: EventAndArgument<E>.() -> S

    override fun build(): Transition<E> {
        val direction: TransitionDirectionProducer<E> = {
            when (it) {
                is DefaultPolicy<E> ->
                    if (it.eventAndArgument.guard())
                        it.targetState(it.eventAndArgument.targetState())
                    else
                        noTransition()
                is CollectTargetStatesPolicy<E> -> noTransition()
            }
        }

        val transition = DefaultTransition(name, eventMatcher, sourceState, direction)
        listener?.let { transition.addListener(it) }
        return transition
    }
}

class ConditionalTransitionBuilder<E : Event>(name: String?, sourceState: IState) :
    TransitionBuilder<E>(name, sourceState) {
    lateinit var direction: EventAndArgument<E>.() -> TransitionDirection

    override fun build(): Transition<E> {
        val direction: TransitionDirectionProducer<E> = {
            when (it) {
                is DefaultPolicy<E> -> it.eventAndArgument.direction()
                is CollectTargetStatesPolicy<E> -> noTransition()
            }
        }

        val transition = DefaultTransition(name, eventMatcher, sourceState, direction)
        listener?.let { transition.addListener(it) }
        return transition
    }
}

/**
 * Any [Event] (with any data) can lead to [State]
 */
class UnitGuardedTransitionBuilder<E : Event>(name: String?, sourceState: IState) :
    GuardedTransitionBuilder<E, State>(name, sourceState)

class UnitGuardedTransitionOnBuilder<E : Event>(name: String?, sourceState: IState) :
    GuardedTransitionOnBuilder<E, State>(name, sourceState)

/**
 * Type safe argument transition builder
 */
class DataGuardedTransitionBuilder<E : DataEvent<D>, D: Any>(name: String?, sourceState: IState) :
    GuardedTransitionBuilder<E, DataState<D>>(name, sourceState)

/**
 * Type safe argument transitionOn builder
 */
class DataGuardedTransitionOnBuilder<E : DataEvent<D>, D: Any>(name: String?, sourceState: IState) :
    GuardedTransitionOnBuilder<E, DataState<D>>(name, sourceState)

inline fun <reified E : Event> TransitionBuilder<E>.onTriggered(crossinline block: (TransitionParams<E>) -> Unit) {
    require(listener == null) { "Listener is already set, only one listener is allowed in a builder" }

    listener = object : Transition.Listener {
        @Suppress("UNCHECKED_CAST")
        override fun onTriggered(transitionParams: TransitionParams<*>) =
            block(transitionParams as TransitionParams<E>)
    }
}

/**
 * Pair of event and argument coming from processEvent() method.
 * Used as single argument in all guard and conditional callbacks same os [TransitionParams] for transitions
 */
data class EventAndArgument<E : Event>(val event: E, val argument: Any?)