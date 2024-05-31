/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.statemachine

import kotlinx.coroutines.CoroutineScope
import ru.nsk.kstatemachine.coroutines.CoroutinesLibCoroutineAbstraction
import ru.nsk.kstatemachine.coroutines.createStateMachine
import ru.nsk.kstatemachine.state.ChildMode

/**
 * Blocking [createStateMachine] alternative
 */
fun createStateMachineBlocking(
    scope: CoroutineScope,
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    start: Boolean = true,
    creationArguments: StateMachine.CreationArguments = StateMachine.CreationArguments(),
    init: suspend BuildingStateMachine.() -> Unit
) = with(CoroutinesLibCoroutineAbstraction(scope)) {
    runBlocking {
        createStateMachine(name, childMode, start, creationArguments, init)
    }
}