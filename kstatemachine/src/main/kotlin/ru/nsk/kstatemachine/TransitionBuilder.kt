package ru.nsk.kstatemachine

import kotlin.reflect.KClass

@StateMachineDslMarker
abstract class TransitionBuilder<E : Event>(protected val name: String?, protected val sourceState: State) {
    var listener: Transition.Listener? = null
    lateinit var eventMatcher: EventMatcher<E>

    abstract fun build(): Transition<E>
}

abstract class BaseGuardedTransitionBuilder<E : Event>(name: String?, sourceState: State) :
    TransitionBuilder<E>(name, sourceState) {
    var guard: () -> Boolean = { true }
}

abstract class GuardedTransitionBuilder<E : Event, S : State>(name: String?, sourceState: State) :
    BaseGuardedTransitionBuilder<E>(name, sourceState) {
    var targetState: S? = null

    override fun build(): Transition<E> {
        val direction = {
            if (guard()) {
                val target = targetState
                if (target == null) stay() else targetState(target)
            } else {
                noTransition()
            }
        }

        val transition = DefaultTransition(name, eventMatcher, sourceState, direction)
        listener?.let { transition.addListener(it) }
        return transition
    }
}

abstract class GuardedTransitionOnBuilder<E : Event, S : State>(name: String?, sourceState: State) :
    BaseGuardedTransitionBuilder<E>(name, sourceState) {
    lateinit var targetState: () -> S

    override fun build(): Transition<E> {
        val direction = {
            if (guard()) targetState(targetState()) else noTransition()
        }

        val transition = DefaultTransition(name, eventMatcher, sourceState, direction)
        listener?.let { transition.addListener(it) }
        return transition
    }
}

class SimpleGuardedTransitionBuilder<E : Event>(name: String?, sourceState: State) :
    GuardedTransitionBuilder<E, State>(name, sourceState)

class SimpleGuardedTransitionOnBuilder<E : Event>(name: String?, sourceState: State) :
    GuardedTransitionOnBuilder<E, State>(name, sourceState)

class ConditionalTransitionBuilder<E : Event>(name: String?, sourceState: State) :
    TransitionBuilder<E>(name, sourceState) {
    lateinit var direction: () -> TransitionDirection

    override fun build(): Transition<E> {
        val transition = DefaultTransition(name, eventMatcher, sourceState, direction)
        listener?.let { transition.addListener(it) }
        return transition
    }
}

/**
 * Type safe argument transition builder
 */
class ArgGuardedTransitionBuilder<E : ArgEvent<A>, A : Any>(name: String?, sourceState: State) :
    GuardedTransitionBuilder<E, ArgState<A>>(name, sourceState)

/**
 * Type safe argument transitionOn builder
 */
class ArgGuardedTransitionOnBuilder<E : ArgEvent<A>, A : Any>(name: String?, sourceState: State) :
    GuardedTransitionOnBuilder<E, ArgState<A>>(name, sourceState)

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