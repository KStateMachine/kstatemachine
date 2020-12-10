package ru.nsk.kstatemachine

import kotlin.reflect.KClass

@StateMachineDslMarker
open class TransitionBuilder<E : Event> {
    var listener: Transition.Listener? = null
    lateinit var eventMatcher: EventMatcher<E>
}

open class SimpleTransitionBuilder<E : Event> :
    TransitionBuilder<E>() {
    var targetState: State? = null
}

class ConditionalTransitionBuilder<E : Event> :
    TransitionBuilder<E>() {
    lateinit var direction: () -> TransitionDirection
}

class GuardedTransitionBuilder<E : Event> :
    SimpleTransitionBuilder<E>() {
    lateinit var guard: () -> Boolean
}

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

inline fun <reified E : Event> TransitionBuilder<E>.isInstanceOf() = EventMatcher.isInstanceOf<E>()
inline fun <reified E : Event> TransitionBuilder<E>.isEqual() = object : EventMatcher<E>(E::class) {
    override fun match(value: Event) = value::class == E::class
}