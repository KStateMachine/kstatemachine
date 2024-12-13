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
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.metainfo.ExportMetaInfoTestData.ValueEvent1
import ru.nsk.kstatemachine.metainfo.ExportMetaInfoTestData.ValueEvent2
import ru.nsk.kstatemachine.state.choiceState
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.state
import ru.nsk.kstatemachine.state.transitionConditionally
import ru.nsk.kstatemachine.state.transitionOn
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.transition.noTransition
import ru.nsk.kstatemachine.transition.stay
import ru.nsk.kstatemachine.transition.targetParallelStates
import ru.nsk.kstatemachine.transition.targetState
import ru.nsk.kstatemachine.visitors.export.exportToPlantUml

private const val EXPORT_META_INFO_TEST = """@startuml
hide empty description
state ExportMetaInfoStateMachine_StateMachine {
    state State1 {
        state State11
        state State12
        
        [*] --> State11
    }
    state State2
    state choiceState <<choice>>
    
    [*] --> State1
}
choiceState --> State1 : if (true)
choiceState --> State2
State1 --> State11 : if (event.value == 42)
State1 --> State12 : else
ExportMetaInfoStateMachine_StateMachine --> State2 : when 2
ExportMetaInfoStateMachine_StateMachine --> State1 : when 1
ExportMetaInfoStateMachine_StateMachine --> State1 : when 3
ExportMetaInfoStateMachine_StateMachine --> State2 : when 3
ExportMetaInfoStateMachine_StateMachine --> ExportMetaInfoStateMachine_StateMachine : when 4
@enduml
"""

private const val EXPORT_META_INFO_WITH_LABELS_TEST = """@startuml
hide empty description
state ExportMetaInfoStateMachine_StateMachine {
    state State1 {
        state State11
        state State12
        
        [*] --> State11
    }
    state State2
    state choiceState <<choice>>
    
    [*] --> State1
}
choiceState --> State1 : if (true)
choiceState --> State2
State1 --> State11 : ValueEvent1, if (event.value == 42)
State1 --> State12 : ValueEvent1, else
ExportMetaInfoStateMachine_StateMachine --> State2 : ValueEvent2, when 2
ExportMetaInfoStateMachine_StateMachine --> State1 : ValueEvent2, when 1
ExportMetaInfoStateMachine_StateMachine --> State1 : ValueEvent2, when 3
ExportMetaInfoStateMachine_StateMachine --> State2 : ValueEvent2, when 3
ExportMetaInfoStateMachine_StateMachine --> ExportMetaInfoStateMachine_StateMachine : ValueEvent2, when 4
@enduml
"""

private const val EXPORT_META_INFO_WITHOUT_UNSAFE_CALL_CONDITIONAL_LAMBDAS_TEST = """@startuml
hide empty description
state ExportMetaInfoStateMachine_StateMachine {
    state State1 {
        state State11
        state State12
        
        [*] --> State11
    }
    state State2
    state choiceState <<choice>>
    
    [*] --> State1
}
choiceState --> State1 : if (true)
choiceState --> State2
ExportMetaInfoStateMachine_StateMachine --> State1 : when 1
ExportMetaInfoStateMachine_StateMachine --> State1 : when 3
ExportMetaInfoStateMachine_StateMachine --> State2 : when 3
ExportMetaInfoStateMachine_StateMachine --> ExportMetaInfoStateMachine_StateMachine : when 4
@enduml
"""

private object ExportMetaInfoTestData {
    class ValueEvent1(val value: Int) : Event
    class ValueEvent2(val value: Int) : Event
}

private suspend fun createTestMachine(coroutineStarterType: CoroutineStarterType): StateMachine {
    return createTestStateMachine(coroutineStarterType, "ExportMetaInfoStateMachine") {
        val state1 = initialState("State1") {
            val state11 = initialState("State11")
            val state12 = state("State12")
            transitionOn<ValueEvent1> {
                targetState = { if (event.value == 42) state11 else state12 }
                metaInfo = buildExportMetaInfo {
                    resolutionHints = setOf(
                        EventAndArgumentResolutionHint("if (event.value == 42)", ValueEvent1(42)),
                        EventAndArgumentResolutionHint("else", ValueEvent1(0)),
                    )
                }
            }
        }
        val state2 = state("State2")

        transitionConditionally<ValueEvent2> {
            direction = {
                when (event.value) {
                    1 -> targetState(state1)
                    2 -> targetState(state2)
                    3 -> targetParallelStates(state1, state2)
                    4 -> stay()
                    else -> noTransition()
                }
            }
            metaInfo = buildExportMetaInfo {
                resolutionHints = setOf(
                    StateResolutionHint("when 1", state1),
                    EventAndArgumentResolutionHint("when 2", ValueEvent2(2)),
                    StateResolutionHint("when 3", setOf(state1, state2)),
                    StateResolutionHint("when 4", this@createTestStateMachine),
                    EventAndArgumentResolutionHint("else", ValueEvent2(5)),
                )
            }
        }
        val choiceState = choiceState("choiceState") { if (true) state1 else state2 }
        choiceState.metaInfo = buildExportMetaInfo {
            resolutionHints = setOf(
                StateResolutionHint("if (true)", state1),
                StateResolutionHint(" ", state2),
            )
        }
    }
}

class ExportMetaInfoTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "ExportMetaInfo test" {
            val machine = createTestMachine(coroutineStarterType)
            machine.exportToPlantUml(unsafeCallConditionalLambdas = true) shouldBe EXPORT_META_INFO_TEST
        }

        "ExportMetaInfo test with labels" {
            val machine = createTestMachine(coroutineStarterType)
            machine.exportToPlantUml(
                showEventLabels = true,
                unsafeCallConditionalLambdas = true
            ) shouldBe EXPORT_META_INFO_WITH_LABELS_TEST
        }

        "ExportMetaInfo test without unsafeCallConditionalLambdas flag" {
            val machine = createTestMachine(coroutineStarterType)
            machine.exportToPlantUml() shouldBe EXPORT_META_INFO_WITHOUT_UNSAFE_CALL_CONDITIONAL_LAMBDAS_TEST
        }
    }
})