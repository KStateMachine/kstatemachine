package ru.nsk.kstatemachine

import kotlin.reflect.KClass

/**
 * Adds an ability to select who [Transition] matches [Event] subclass
 */
abstract class EventMatcher<E : Event>(val eventClass: KClass<E>) {
    abstract suspend fun match(value: Event): Boolean

    companion object {
        /** This matcher is used by default, allowing [Event] subclasses */
        inline fun <reified E : Event> isInstanceOf() = object : EventMatcher<E>(E::class) {
            override suspend fun match(value: Event) = value is E
        }
    }
}

@Suppress("UNUSED") // The unused warning is probably a bug
inline fun <reified E : Event> TransitionBuilder<E>.isInstanceOf() = EventMatcher.isInstanceOf<E>()

@Suppress("UNUSED") // The unused warning is probably a bug
inline fun <reified E : Event> TransitionBuilder<E>.isEqual() = object : EventMatcher<E>(E::class) {
    override suspend fun match(value: Event) = value::class == E::class
}

fun finishedEventMatcher(state: IState) = object : EventMatcher<FinishedEvent>(FinishedEvent::class) {
    override suspend fun match(value: Event) = if (value is FinishedEvent) value.state == state else false
}