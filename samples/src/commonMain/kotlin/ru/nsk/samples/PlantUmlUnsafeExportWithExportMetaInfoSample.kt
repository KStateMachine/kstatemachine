/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.samples

import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.metainfo.ExportMetaInfo
import ru.nsk.kstatemachine.metainfo.buildExportMetaInfo
import ru.nsk.kstatemachine.state.State
import ru.nsk.kstatemachine.state.finalState
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.transitionOn
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.visitors.export.exportToPlantUml
import ru.nsk.samples.PlantUmlUnsafeExportWithExportMetaInfoSample.FirstEvent
import ru.nsk.samples.PlantUmlUnsafeExportWithExportMetaInfoSample.SecondEvent

private object PlantUmlUnsafeExportWithExportMetaInfoSample {
    class FirstEvent(val data: Int) : Event
    class SecondEvent(val data: Int) : Event
}

/**
 * The sample shows how to use [ExportMetaInfo] to get complete export output even with conditional lambdas.
 * You have EventAndArgumentResolutionHint and EventAndArgumentResolutionHint alternative, you can choose one of them,
 * or use them together mixing in a single metaInfo if necessary.
 */
fun main() = runBlocking {
    lateinit var state2: State
    lateinit var state3: State
    val machine = createStateMachine(this) {
        state2 = finalState("State2")
        state3 = finalState("State3")

        initialState("State1") {
            transitionOn<FirstEvent> {
                metaInfo = buildExportMetaInfo {
                    // using EventAndArgumentResolutionHint calls targetState lambda with specified events
                    addEventAndArgumentResolutionHint("data == 42", FirstEvent(42))
                    addEventAndArgumentResolutionHint("else", FirstEvent(42))
                }
                targetState = { if (event.data == 42) state2 else state3 }
            }

            transitionOn<SecondEvent> {
                metaInfo = buildExportMetaInfo {
                    // using StateResolutionHint does not require targetState lambda call
                    addStateResolutionHint("data == 123", state2)
                    addStateResolutionHint("else", state3)
                }
                targetState = { if (event.data == 123) state2 else state3 }
            }
        }
    }

    println(machine.exportToPlantUml(showEventLabels = true, unsafeCallConditionalLambdas = true))
}