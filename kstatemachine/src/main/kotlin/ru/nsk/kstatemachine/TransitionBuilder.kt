package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.TransitionDirectionProducerPolicy.CollectTargetStatesPolicy
import ru.nsk.kstatemachine.TransitionDirectionProducerPolicy.DefaultPolicy
import kotlin.reflect.KClass

@StateMachineDslMarker
abstract class TransitionBuilder<E : Event>(protected val name: String?, protected val sourceState: IState) {
    var listener: Transition.Listener? = null
    lateinit var eventMatcher: EventMatcher<E>

    abstract fun build(): Transition<E>
}

abstract class BaseGuardedTransitionBuilder<E : Event>(name: String?, sourceState: IState) :
    TransitionBuilder<E>(name, sourceState) {
    var guard: (E) -> Boolean = { true }
}

abstract class GuardedTransitionBuilder<E : Event, S : IState>(name: String?, sourceState: IState) :
    BaseGuardedTransitionBuilder<E>(name, sourceState) {
    var targetState: S? = null

    override fun build(): Transition<E> {
        val direction: TransitionDirectionProducer<E> = {
            when (it) {
                is DefaultPolicy<E> ->
                    if (guard(it.event)) {
                        val target = targetState
                        if (target == null) stay() else targetState(target)
                    } else {
                        noTransition()
                    }
                is CollectTargetStatesPolicy<E> -> {
                    val target = targetState
                    if (target == null) stay() else targetState(target)
                }
            }
        }

        val transition = DefaultTransition(name, eventMatcher, sourceState, direction)
        listener?.let { transition.addListener(it) }
        return transition
    }
}

abstract class GuardedTransitionOnBuilder<E : Event, S : IState>(name: String?, sourceState: IState) :
    BaseGuardedTransitionBuilder<E>(name, sourceState) {
    lateinit var targetState: (E) -> S

    override fun build(): Transition<E> {
        val direction: TransitionDirectionProducer<E> = {
            when (it) {
                is DefaultPolicy<E> -> if (guard(it.event)) targetState(targetState(it.event)) else noTransition()
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
    lateinit var direction: (E) -> TransitionDirection

    override fun build(): Transition<E> {
        val direction: TransitionDirectionProducer<E> = {
            when (it) {
                is DefaultPolicy<E> -> direction(it.event)
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
class DataGuardedTransitionBuilder<E : DataEvent<D>, D>(name: String?, sourceState: IState) :
    GuardedTransitionBuilder<E, DataState<D>>(name, sourceState)

/**
 * Type safe argument transitionOn builder
 */
class DataGuardedTransitionOnBuilder<E : DataEvent<D>, D>(name: String?, sourceState: IState) :
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
 * Adds an ability to select who [Transition] matches [Event] subclass
 */
abstract class EventMatcher<E : Event>(val eventClass: KClass<E>) {
    abstract fun match(value: Event): Boolean

    companion object {
        /** This matcher is used by default, allowing [Event] subclasses */
        inline fun <reified E : Event> isInstanceOf() = object : EventMatcher<E>(E::class) {
            override fun match(value: Event) = value is E
        }
    }
}

@Suppress("UNUSED") // The unused warning is probably a bug
inline fun <reified E : Event> TransitionBuilder<E>.isInstanceOf() = EventMatcher.isInstanceOf<E>()

@Suppress("UNUSED") // The unused warning is probably a bug
inline fun <reified E : Event> TransitionBuilder<E>.isEqual() = object : EventMatcher<E>(E::class) {
    override fun match(value: Event) = value::class == E::class
}