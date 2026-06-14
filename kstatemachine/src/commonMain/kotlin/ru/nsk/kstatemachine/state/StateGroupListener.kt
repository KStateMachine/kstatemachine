/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2026.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import ru.nsk.kstatemachine.transition.TransitionParams

/**
 * Triggers [onChanged] callback when condition "all passed states are active" changes.
 *
 * [onChanged] is fired from the watched states' suspending `onEntry`/`onExit` callbacks (and once
 * synchronously at subscribe time if [notifyOnSubscribe] is `true`), so it may itself suspend.
 */
suspend fun onActiveAllOf(
    mandatoryState1: IState,
    mandatoryState2: IState,
    vararg otherStates: IState,
    notifyOnSubscribe: Boolean = false,
    onChanged: suspend (Boolean) -> Unit
): GroupListener =
    onActiveAllOf(setOf(mandatoryState1, mandatoryState2, *otherStates), notifyOnSubscribe, onChanged)

suspend fun onActiveAllOf(
    states: Set<IState>,
    notifyOnSubscribe: Boolean = false,
    onChanged: suspend (Boolean) -> Unit
): GroupListener {
    require(states.size >= 2) {
        "There is no sense to use this API with less than 2 unique states, did you passed same state more then once?"
    }
    val initialActiveCount = states.countActive()
    val initialStatus = initialActiveCount == states.size

    val listener = object : StateGroupListener {
        private var activeCount = initialActiveCount
        private var status = initialStatus

        override suspend fun onEntry(transitionParams: TransitionParams<*>) {
            ++activeCount
            updateStatus()
        }

        override suspend fun onExit(transitionParams: TransitionParams<*>) {
            --activeCount
            updateStatus()
        }

        private suspend fun updateStatus() {
            val newStatus = activeCount == states.size
            if (status != newStatus) {
                status = newStatus
                onChanged(newStatus)
            }
        }

        override fun unsubscribe() {
            states.forEach { it.removeListener(this) }
        }
    }

    if (notifyOnSubscribe) onChanged(initialStatus)
    states.forEach { it.addListener(listener) }
    return listener
}

/**
 * Triggers [onChanged] callback when condition "any of passed states is active" changes.
 *
 * [onChanged] is fired from the watched states' suspending `onEntry`/`onExit` callbacks (and once
 * synchronously at subscribe time if [notifyOnSubscribe] is `true`), so it may itself suspend.
 */
suspend fun onActiveAnyOf(
    mandatoryState1: IState,
    mandatoryState2: IState,
    vararg otherStates: IState,
    notifyOnSubscribe: Boolean = false,
    onChanged: suspend (Boolean) -> Unit
): GroupListener =
    onActiveAnyOf(setOf(mandatoryState1, mandatoryState2, *otherStates), notifyOnSubscribe, onChanged)


suspend fun onActiveAnyOf(
    states: Set<IState>,
    notifyOnSubscribe: Boolean = false,
    onChanged: suspend (Boolean) -> Unit
): GroupListener {
    require(states.size >= 2) {
        "There is no sense to use this API with less than 2 unique states, did you passed same state more then once?"
    }
    val initialStatus = states.any { it.isActive }

    val listener = object : StateGroupListener {
        private var status = initialStatus

        override suspend fun onEntry(transitionParams: TransitionParams<*>) = updateStatus()
        override suspend fun onExit(transitionParams: TransitionParams<*>) = updateStatus()

        private suspend fun updateStatus() {
            val newStatus = states.any { it.isActive }
            if (status != newStatus) {
                status = newStatus
                onChanged(newStatus)
            }
        }

        override fun unsubscribe() {
            states.forEach { it.removeListener(this) }
        }
    }

    if (notifyOnSubscribe) onChanged(initialStatus)
    states.forEach { it.addListener(listener) }
    return listener
}
