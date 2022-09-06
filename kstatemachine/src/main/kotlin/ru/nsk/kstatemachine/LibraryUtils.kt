package ru.nsk.kstatemachine

internal inline fun <T, R : Any> Iterable<T>.firstNotNullOfOrNull(transform: (T) -> R?): R? {
    for (element in this) {
        val result = transform(element)
        if (result != null)
            return result
    }
    return null
}

/**
 * [forEach] analog which ignores internal state machines
 */
internal fun Iterable<InternalState>.forEachState(block: (InternalState) -> Unit) {
    forEach { if (it !is StateMachine) block(it) }
}

internal fun IState.isSubStateOf(state: IState): Boolean {
    state.states.forEach {
        if (it === this)
            return true
        else if (it !is StateMachine && this.isSubStateOf(it)) // do not process sub-states of composed machines
            return true
    }
    return false
}