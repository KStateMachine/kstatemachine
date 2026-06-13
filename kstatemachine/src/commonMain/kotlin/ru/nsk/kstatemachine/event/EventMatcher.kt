/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.event

import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.transition.Transition
import ru.nsk.kstatemachine.transition.TransitionBuilder
import kotlin.reflect.KClass

/**
 * Adds an ability to select who [Transition] matches [Event] subclass
 */
interface EventMatcher<E : Event> {
    /** Never match by accessing this field. Use [match] */
    val eventClass: KClass<E>
    suspend fun match(value: Event): Boolean

    companion object {
        /** This matcher is used by default, allowing [Event] subclasses */
        inline fun <reified E : Event> isInstanceOf() = object : EventMatcher<E> {
            override val eventClass = E::class
            override suspend fun match(value: Event) = value is E
        }
    }
}

@Suppress("UNUSED") // The unused warning is probably a bug
inline fun <reified E : Event> TransitionBuilder<E>.isInstanceOf() = EventMatcher.isInstanceOf<E>()

@Suppress("UNUSED") // The unused warning is probably a bug
inline fun <reified E : Event> TransitionBuilder<E>.isEqual() = object : EventMatcher<E> {
    override val eventClass = E::class
    override suspend fun match(value: Event) = value::class == E::class
}

fun finishedEventMatcher(state: IState) = object : EventMatcher<FinishedEvent> {
    override val eventClass = FinishedEvent::class
    override suspend fun match(value: Event) = value is FinishedEvent && value.state === state
}

internal fun joinEventMatcher(joinId: Any): EventMatcher<JoinCompleteEvent> =
    object : EventMatcher<JoinCompleteEvent> {
        override val eventClass = JoinCompleteEvent::class
        override suspend fun match(value: Event) =
            value is JoinCompleteEvent && value.joinId === joinId
    }

internal fun autoEventMatcher(transitionId: Any): EventMatcher<AutoEvent> =
    object : EventMatcher<AutoEvent> {
        override val eventClass = AutoEvent::class
        override suspend fun match(value: Event) =
            value is AutoEvent && value.transitionId === transitionId
    }

