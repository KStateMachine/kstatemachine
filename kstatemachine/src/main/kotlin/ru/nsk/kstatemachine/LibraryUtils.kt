package ru.nsk.kstatemachine

/**
 * [forEach] analog which ignores internal state machines
 */
internal suspend fun Iterable<InternalState>.forEachState(block: suspend (InternalState) -> Unit) {
    forEach { if (it !is StateMachine) block(it) }
}

fun IState.isNeighbor(state: IState) = parent?.states?.contains(state) == true

fun IState.isSubStateOf(state: IState): Boolean {
    state.states.forEach {
        if (it === this)
            return true
        else if (it !is StateMachine && this.isSubStateOf(it)) // do not process sub-states of composed machines
            return true
    }
    return false
}