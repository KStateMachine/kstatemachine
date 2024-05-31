/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.state.InternalState
import ru.nsk.kstatemachine.statemachine.StateMachine

/**
 * [forEach] analog which ignores internal [StateMachine]s
 */
internal suspend fun Iterable<InternalState>.forEachState(block: suspend (InternalState) -> Unit) {
    forEach { if (it !is StateMachine) block(it) }
}

fun IState.isNeighbor(state: IState) = parent?.states?.contains(state) == true

fun IState.isSubStateOf(state: IState): Boolean {
    state.states.forEach {
        if (it === this)
            return true
        else if (it !is StateMachine && this.isSubStateOf(it)) // do not process child states of composed machines
            return true
    }
    return false
}