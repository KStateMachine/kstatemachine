package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlin.coroutines.Continuation

class CoroutinesLibraryTest : StringSpec({
    "test coroutines called from machine callbacks" {
        val machine = createCoStateMachine(GlobalScope) {
            onStarted { delay(1) }
            onStopped { delay(1) }
            initialState("first") {
                onEntry { delay(1) }
                onExit { delay(1) }
            }
        }
        //println("test processEvent ${Thread.currentThread()}")
        //machine.processEvent(SwitchEvent)
    }
})
