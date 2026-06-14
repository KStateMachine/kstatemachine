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

internal fun joinEventMatcher(transitionId: Any): EventMatcher<JoinCompleteEvent> =
    object : EventMatcher<JoinCompleteEvent> {
        override val eventClass = JoinCompleteEvent::class
        override suspend fun match(value: Event) =
            value is JoinCompleteEvent && value.transitionId === transitionId
    }

internal fun <D: Any> joinDataEventMatcher(transitionId: Any): EventMatcher<JoinCompleteDataEvent<D>> =
    object : EventMatcher<JoinCompleteDataEvent<D>> {
        @Suppress("UNCHECKED_CAST")
        override val eventClass = JoinCompleteEvent::class as KClass<JoinCompleteDataEvent<D>>
        override suspend fun match(value: Event) =
            value is JoinCompleteDataEvent<*> && value.transitionId === transitionId
    }

internal fun autoEventMatcher(transitionId: Any): EventMatcher<AutoEvent> =
    object : EventMatcher<AutoEvent> {
        override val eventClass = AutoEvent::class
        override suspend fun match(value: Event) =
            value is AutoEvent && value.transitionId === transitionId
    }

internal fun <D: Any> autoDataEventMatcher(transitionId: Any): EventMatcher<AutoDataEvent<D>> =
    object : EventMatcher<AutoDataEvent<D>> {
        @Suppress("UNCHECKED_CAST")
        override val eventClass = AutoDataEvent::class as KClass<AutoDataEvent<D>>
        override suspend fun match(value: Event) =
            value is AutoDataEvent<*> && value.transitionId === transitionId
    }

/**
 * Library-internal. Public only because `kstatemachine-coroutines` constructs delayed transitions
 * (Kotlin `internal` is per-module). Do not call from user code.
 */
fun delayedEventMatcher(transitionId: Any): EventMatcher<DelayedTransitionEvent> =
    object : EventMatcher<DelayedTransitionEvent> {
        override val eventClass = DelayedTransitionEvent::class
        override suspend fun match(value: Event) =
            value is DelayedTransitionEvent && value.transitionId === transitionId
    }
