/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.coroutines

import kotlinx.coroutines.CoroutineScope
import ru.nsk.kstatemachine.statemachine.StateMachine
import kotlin.coroutines.CoroutineContext

internal actual fun <T> doRunBlocking(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T =
    throw UnsupportedOperationException(
        "This platform does not support kotlinx.coroutines.runBlocking(). " +
                "Do not use blocking API or use ${StateMachine::class.simpleName} without coroutines support"
    )