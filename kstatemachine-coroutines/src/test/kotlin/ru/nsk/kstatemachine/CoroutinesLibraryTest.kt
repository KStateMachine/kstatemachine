package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.*
import kotlin.coroutines.Continuation

class CoroutinesLibraryTest : StringSpec({
    "test coroutines called from machine callbacks" {
        createCoStateMachine(GlobalScope) {
            onStarted { delay(1) }
            initialState("first") {
                onEntry {
                    coroutineScope {
                        launch { delay(1) }
                        launch { delay(1) }
                    }
                    withContext(Dispatchers.Default) {
                        delay(1)
                    }
                }
            }
        }
    }
})
