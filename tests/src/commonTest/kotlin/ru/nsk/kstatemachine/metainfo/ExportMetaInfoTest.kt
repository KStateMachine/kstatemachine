/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.metainfo

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ru.nsk.kstatemachine.CoroutineStarterType
import ru.nsk.kstatemachine.SwitchEvent
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.state.choiceState
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.state
import ru.nsk.kstatemachine.state.transitionConditionally
import ru.nsk.kstatemachine.state.transitionOn
import ru.nsk.kstatemachine.transition.EventAndArgument
import ru.nsk.kstatemachine.transition.noTransition
import ru.nsk.kstatemachine.transition.stay
import ru.nsk.kstatemachine.transition.targetParallelStates
import ru.nsk.kstatemachine.transition.targetState
import ru.nsk.kstatemachine.visitors.export.exportToPlantUml

private const val TEST = """
    TODO
"""

class ExportMetaInfoTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "ExportMetaInfo test" {
            val machine = createTestStateMachine(coroutineStarterType) {
                val state1 = initialState("State1")
                val state2 = state("State2")
                transitionOn<SwitchEvent> {
                    targetState = {
                        if (false) state1 else state2
                    }
                    metaInfo = buildExportMetaInfo {
                        resolutionHints = setOf(
                            StateResolutionHint("if (false)",state1),
                            StateResolutionHint("else", state2),
                        )
                    }
                }
                transitionConditionally<SwitchEvent> {
                    direction = {
                        when(0) {
                            1 -> targetState(state1)
                            2 -> targetState(state2)
                            3 -> targetParallelStates(state1, state2)
                            4 -> stay()
                            else -> noTransition()
                        }
                    }
                    // provide state results
                    metaInfo = buildExportMetaInfo {
                        resolutionHints = setOf(
                            StateResolutionHint("when 1", state1),
                            StateResolutionHint("when 2", state2),
                            StateResolutionHint("when 2", setOf(state1, state2)), // for parallel
                        )
                    }
                    // provide direction results
                    metaInfo = buildExportMetaInfo {
                        resolutionHints = setOf(
//                            DirectionResolutionHint(targetState(state1)),
//                            DirectionResolutionHint(targetState(state2)),
//                            DirectionResolutionHint(targetParallelStates(state1, state2)),
                            DirectionResolutionHint("when 4", stay()),
                            // does not make sense to use DirectionResolutionHint("else", noTransition()),
                        )
                    }
                    // help with calculation
                    metaInfo = buildExportMetaInfo {
                        resolutionHints = setOf(
                            EventAndArgumentResolutionHint("SwitchEvent", SwitchEvent),
                            EventAndArgumentResolutionHint("SwitchEvent", SwitchEvent),
                        )
                    }

                    // mix help approaches
                    metaInfo = buildExportMetaInfo {
                        resolutionHints = setOf(
                            StateResolutionHint("", state1),
                            DirectionResolutionHint("", stay()),
                            EventAndArgumentResolutionHint("", SwitchEvent),
                        )
                    }
                }
                choiceState("choice") {
                    metaInfo = buildExportMetaInfo {
                        resolutionHints = setOf(
                            StateResolutionHint("if (true)", state1),
                            StateResolutionHint("else", state2),
                        )
                    }
                    if (true) state1 else state2
                }
            }
            machine.exportToPlantUml() shouldBe TEST
        }
    }
})