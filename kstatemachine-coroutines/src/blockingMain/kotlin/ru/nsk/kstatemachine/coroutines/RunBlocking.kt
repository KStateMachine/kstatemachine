package ru.nsk.kstatemachine.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

internal actual fun <T> doRunBlocking(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T =
    runBlocking(context, block)