package ru.nsk.kstatemachine.transition

import ru.nsk.kstatemachine.event.DataEvent
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.event.EventMatcher
import ru.nsk.kstatemachine.metainfo.MetaInfo
import ru.nsk.kstatemachine.state.DataState
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.state.State
import ru.nsk.kstatemachine.statemachine.StateMachineDslMarker
import ru.nsk.kstatemachine.transition.TransitionDirectionProducerPolicy.*

@StateMachineDslMarker
abstract class TransitionBuilder<E : Event>(protected val name: String?, protected val sourceState: IState) {
    val listeners = mutableListOf<Transition.Listener>()
    lateinit var eventMatcher: EventMatcher<E>
    var type = TransitionType.LOCAL
    var metaInfo: MetaInfo? = null

    abstract fun build(): Transition<E>
}

abstract class BaseGuardedTransitionBuilder<E : Event>(name: String?, sourceState: IState) :
    TransitionBuilder<E>(name, sourceState) {
    var guard: suspend EventAndArgument<E>.() -> Boolean = { true }
}

abstract class GuardedTransitionBuilder<E : Event, S : IState>(name: String?, sourceState: IState) :
    BaseGuardedTransitionBuilder<E>(name, sourceState) {
    var targetState: S? = null

    override fun build(): Transition<E> {
        val direction: TransitionDirectionProducer<E> = {
            when (it) {
                is DefaultPolicy ->
                    if (it.eventAndArgument.guard())
                        it.targetStateOrStay(targetState)
                    else
                        noTransition()
                is CollectTargetStatesPolicy,
                is UnsafeCollectTargetStatesPolicy -> it.targetStateOrStay(targetState)
            }
        }

        val transition = DefaultTransition(name, eventMatcher, type, metaInfo, sourceState, direction)
        listeners.forEach { transition.addListener(it) }
        return transition
    }
}

abstract class GuardedTransitionOnBuilder<E : Event, S : IState>(name: String?, sourceState: IState) :
    BaseGuardedTransitionBuilder<E>(name, sourceState) {
    lateinit var targetState: suspend EventAndArgument<E>.() -> S

    override fun build(): Transition<E> {
        val direction: TransitionDirectionProducer<E> = { policy ->
            when (policy) {
                is DefaultPolicy ->
                    if (policy.eventAndArgument.guard())
                        policy.targetState(policy.eventAndArgument.targetState())
                    else
                        noTransition()
                is CollectTargetStatesPolicy -> noTransition()
                is UnsafeCollectTargetStatesPolicy -> policy.targetState(policy.eventAndArgument.targetState())
            }
        }

        val transition = DefaultTransition(name, eventMatcher, type, metaInfo, sourceState, direction)
        listeners.forEach { transition.addListener(it) }
        return transition
    }
}

class ConditionalTransitionBuilder<E : Event>(name: String?, sourceState: IState) :
    TransitionBuilder<E>(name, sourceState) {
    lateinit var direction: suspend EventAndArgument<E>.() -> TransitionDirection

    override fun build(): Transition<E> {
        val direction: TransitionDirectionProducer<E> = { policy ->
            when (policy) {
                is DefaultPolicy -> policy.eventAndArgument.direction()
                is CollectTargetStatesPolicy -> noTransition()
                is UnsafeCollectTargetStatesPolicy -> policy.eventAndArgument.direction()
            }
        }

        val transition = DefaultTransition(name, eventMatcher, type, metaInfo, sourceState, direction)
        listeners.forEach { transition.addListener(it) }
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
class DataGuardedTransitionBuilder<E : DataEvent<D>, D : Any>(name: String?, sourceState: IState) :
    BaseGuardedTransitionBuilder<E>(name, sourceState) {
    /** User should initialize this filed */
    lateinit var targetState: DataState<D>

    override fun build(): Transition<E> {
        require(this::targetState.isInitialized) { "targetState should be set in this transition builder" }
        val direction: TransitionDirectionProducer<E> = { policy ->
            when (policy) {
                is DefaultPolicy ->
                    if (policy.eventAndArgument.guard())
                        policy.targetState(targetState)
                    else
                        noTransition()
                is CollectTargetStatesPolicy,
                is UnsafeCollectTargetStatesPolicy -> policy.targetState(targetState)
            }
        }

        val transition = DefaultTransition(name, eventMatcher, type, metaInfo, sourceState, direction)
        listeners.forEach { transition.addListener(it) }
        return transition
    }
}

/**
 * Type safe argument transitionOn builder
 */
class DataGuardedTransitionOnBuilder<E : DataEvent<D>, D : Any>(name: String?, sourceState: IState) :
    GuardedTransitionOnBuilder<E, DataState<D>>(name, sourceState)

inline fun <reified E : Event> TransitionBuilder<E>.onTriggered(
    crossinline block: suspend (TransitionParams<E>) -> Unit
): Transition.Listener {
    return object : Transition.Listener {
        @Suppress("UNCHECKED_CAST")
        override suspend fun onTriggered(transitionParams: TransitionParams<*>) =
            block(transitionParams as TransitionParams<E>)
    }.also { listeners += it }
}

inline fun <reified E : Event> TransitionBuilder<E>.onComplete(
    // arg names are provided for better syntax highlighting
    crossinline block: suspend (transitionParams: TransitionParams<E>, activeStates: Set<IState>) -> Unit
): Transition.Listener {
    return object : Transition.Listener {
        @Suppress("UNCHECKED_CAST")
        override suspend fun onComplete(transitionParams: TransitionParams<*>, activeStates: Set<IState>) =
            block(transitionParams as TransitionParams<E>, activeStates)
    }.also { listeners += it }
}

/**
 * Pair of event and argument coming from processEvent() method.
 * Used as single argument in all guard and conditional callbacks same as [TransitionParams] for transitions
 */
data class EventAndArgument<E : Event>(val event: E, val argument: Any?)