package ru.nsk.kstatemachine

@StateMachineDslMarker
open class TransitionBuilder<out E : Event> {
    var listener: Transition.Listener? = null
    lateinit var eventMatcher: EventMatcher
}

class SimpleTransitionBuilder<E : Event> :
    TransitionBuilder<E>() {
    var targetState: State? = null
}

class ConditionalTransitionBuilder<E : Event> :
    TransitionBuilder<E>() {
    lateinit var direction: () -> TransitionDirection
}

inline fun <reified E : Event> TransitionBuilder<E>.onTriggered(crossinline block: (TransitionParams<E>) -> Unit) {
    require(listener == null) { "Listener is already set, only one listener is allowed in a builder" }

    listener = object : Transition.Listener {
        override fun onTriggered(transitionParams: TransitionParams<*>) =
            block(transitionParams as TransitionParams<E>)
    }
}

/**
 * Adds an ability to select who [Transition] matches [Event] subclass
 */
fun interface EventMatcher {
    fun match(value: Event): Boolean

    companion object {
        /** This matcher is used by default, allowing [Event] subclasses */
        inline fun <reified E : Event> isInstanceOf() = EventMatcher { it is E }
    }
}

inline fun <reified E : Event> TransitionBuilder<E>.isInstanceOf() = EventMatcher.isInstanceOf<E>()
inline fun <reified E : Event> TransitionBuilder<E>.isEqual() = EventMatcher { it.javaClass.kotlin == E::class }