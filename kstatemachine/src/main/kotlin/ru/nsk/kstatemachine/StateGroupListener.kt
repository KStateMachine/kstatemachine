package ru.nsk.kstatemachine

import kotlin.properties.Delegates.observable

interface GroupListener {
    fun unsubscribe()
}

private interface StateGroupListener : IState.Listener, GroupListener

/**
 * Triggers [onChanged] callback when condition "all passed states are active" changes
 */
fun onActiveAllOf(
    mandatoryState1: IState,
    mandatoryState2: IState,
    vararg otherStates: IState,
    notifyOnSubscribe: Boolean = false,
    onChanged: (Boolean) -> Unit
): GroupListener {
    val allStates = setOf(mandatoryState1, mandatoryState2, *otherStates)
    return onActiveAllOf(allStates, notifyOnSubscribe, onChanged)
}

fun onActiveAllOf(
    states: Set<IState>,
    notifyOnSubscribe: Boolean = false,
    onChanged: (Boolean) -> Unit
): GroupListener {
    require(states.size >= 2) {
        "There is no sense to use this API with less than 2 unique states, did you passed same state more then once?"
    }
    val initialActiveCount = states.countActive()

    val listener = object : StateGroupListener {
        private var status by observable(initialActiveCount == states.size) { _, oldValue, newValue ->
            if (oldValue != newValue) onChanged(newValue)
        }

        private var activeCount = initialActiveCount
            set(value) {
                field = value
                status = states.countActive() == states.size
            }

        init {
            if (notifyOnSubscribe) onChanged(status)
        }

        override suspend fun onEntry(transitionParams: TransitionParams<*>) {
            ++activeCount
        }

        override suspend fun onExit(transitionParams: TransitionParams<*>) {
            --activeCount
        }

        override fun unsubscribe() {
            states.forEach { it.removeListener(this) }
        }
    }

    states.forEach { it.addListener(listener) }
    return listener
}

private fun Iterable<IState>.countActive() = count { it.isActive }

/**
 * Triggers [onChanged] callback when condition "any of passed states is active" changes
 */
fun onActiveAnyOf(
    mandatoryState1: IState,
    mandatoryState2: IState,
    vararg otherStates: IState,
    notifyOnSubscribe: Boolean = false,
    onChanged: (Boolean) -> Unit
): GroupListener {
    val allStates = setOf(mandatoryState1, mandatoryState2, *otherStates)
    return onActiveAnyOf(allStates, notifyOnSubscribe, onChanged)
}

fun onActiveAnyOf(
    states: Set<IState>,
    notifyOnSubscribe: Boolean = false,
    onChanged: (Boolean) -> Unit
): GroupListener {
    require(states.size >= 2) {
        "There is no sense to use this API with less than 2 unique states, did you passed same state more then once?"
    }

    val listener = object : StateGroupListener {
        private var status by observable(calculateStatus()) { _, oldValue, newValue ->
            if (oldValue != newValue) onChanged(newValue)
        }

        init {
            if (notifyOnSubscribe) onChanged(status)
        }

        private fun updateStatus() {
            status = calculateStatus()
        }

        private fun calculateStatus() = states.firstOrNull { it.isActive } != null

        override suspend fun onEntry(transitionParams: TransitionParams<*>) = updateStatus()
        override suspend fun onExit(transitionParams: TransitionParams<*>) = updateStatus()

        override fun unsubscribe() {
            states.forEach { it.removeListener(this) }
        }
    }

    states.forEach { it.addListener(listener) }
    return listener
}