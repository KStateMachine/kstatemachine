/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

internal actual fun <T> doRunBlocking(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T =
    runBlocking(context, block)