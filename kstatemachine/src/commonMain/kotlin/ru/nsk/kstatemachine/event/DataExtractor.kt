/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.event

import ru.nsk.kstatemachine.state.DataState
import ru.nsk.kstatemachine.transition.TransitionParams
import kotlin.reflect.KClass

/**
 * Allows to extract data for [DataState] from any [Event]
 *
 * [FinishedEvent] handling is separated into special method as it is very easy to forget to handle it in single method
 * when implementing custom [DataExtractor].
 */
interface DataExtractor<D : Any> {
    val dataClass: KClass<D>
    suspend fun extractFinishedEvent(transitionParams: TransitionParams<*>, event: FinishedEvent): D?
    suspend fun extract(transitionParams: TransitionParams<*>, isImplicitActivation: Boolean): D?
}

inline fun <reified D : Any> defaultDataExtractor() = object : DataExtractor<D> {
    override val dataClass = D::class

    override suspend fun extractFinishedEvent(transitionParams: TransitionParams<*>, event: FinishedEvent) =
        event.data as? D

    /**
     * [isImplicitActivation] is true if if the state is activated by transition activating the state non-directly
     * (targeting [DataState]'s substate)
     */
    override suspend fun extract(transitionParams: TransitionParams<*>, isImplicitActivation: Boolean) = null
}