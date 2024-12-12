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
import ru.nsk.kstatemachine.state.finalState
import ru.nsk.kstatemachine.state.initialChoiceState
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.invoke
import ru.nsk.kstatemachine.state.state
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.visitors.export.exportToPlantUml

private const val PLANTUML_META_INFO = """@startuml
hide empty description
state "Nested states sm" as Meta_info_StateMachine {
    state State_1 {
        state State12
        state "Choice label" as ChoiceState <<choice>>
        note right of ChoiceState : Note 1
        note right of ChoiceState : Note 2
        
        [*] --> ChoiceState
    }
    state "Long State 3" as State3
    State3 : Description 1
    State3 : Description 2
    note right of State3 : Note 1
    note right of State3 : Note 2
    state "Long State 2" as State2 {
        state "Final sub state" as FinalState
        state Initial_subState
        
        [*] --> Initial_subState
        Initial_subState --> FinalState : SwitchEvent
        FinalState --> [*]
    }
    
    [*] --> State_1
    State_1 --> State2 : go to State2, SwitchEvent
    State_1 --> State_1 : self targeted, SwitchEvent
    note on link
        Note 1
    end note
    note on link
        Note 2
    end note
    State_1 --> State_1 : SwitchEvent
    State2 --> State3 : That's all, SwitchEvent
    State2 --> State_1 : back to State 1, SwitchEvent
    State3 --> [*]
}
ChoiceState --> State12
@enduml
"""

private const val PLANTUML_COMPOSITE_META_INFO = """@startuml
hide empty description
state "Nested states sm" as Meta_info_StateMachine {
    state State1
    
    [*] --> State1
}
@enduml
"""

class UmlMetaInfoTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "metaInfo export test" {
            val machine = createTestStateMachine(coroutineStarterType, name = "Meta info") {
                // label for state machine
                metaInfo = buildUmlMetaInfo { umlLabel = "Nested states sm" }

                val state1 = initialState("State-1")
                val state3 = finalState("State3") {
                    // label for state
                    metaInfo = buildUmlMetaInfo {
                        umlLabel = "Long State 3"
                        umlStateDescriptions = listOf("Description 1", "Description 2")
                        umlNotes = listOf("Note 1", "Note 2")
                    }
                }

                val state2 = state("State2") {
                    // label for state
                    metaInfo = buildUmlMetaInfo { umlLabel = "Long State 2" }
                    transition<SwitchEvent> {
                        // label for transition
                        metaInfo = buildUmlMetaInfo { umlLabel = "That's all" }
                        targetState = state3
                    }
                    transition<SwitchEvent>("back") {
                        // label for transition
                        metaInfo = buildUmlMetaInfo { umlLabel = "back to State 1" }
                        targetState = state1
                    }
                    val finalSubState = finalState("FinalState") {
                        // label for state
                        metaInfo = buildUmlMetaInfo { umlLabel = "Final sub state" }
                    }
                    initialState("Initial subState") {
                        transition<SwitchEvent> { targetState = finalSubState }
                    }
                }

                state1 {
                    transition<SwitchEvent> {
                        metaInfo = buildUmlMetaInfo { umlLabel = "go to ${state2.name}" }
                        targetState = state2
                    }
                    transition<SwitchEvent>("self targeted") {
                        targetState = this@state1
                        metaInfo = buildUmlMetaInfo { umlNotes = listOf("Note 1", "Note 2") }
                    }
                    transition<SwitchEvent>()

                    val state12 = state("State12")
                    val choiceState = initialChoiceState("ChoiceState") { state12 }
                    choiceState.metaInfo = buildUmlMetaInfo {
                        umlLabel = "Choice label"
                        // no plantUml nor Mermaid can draw this
                        umlStateDescriptions = listOf("Description 1", "Description 2")
                        umlNotes = listOf("Note 1", "Note 2")
                    }
                }
            }

            machine.exportToPlantUml(
                showEventLabels = true,
                unsafeCallConditionalLambdas = true
            ) shouldBe PLANTUML_META_INFO
        }

        "CompositeMetaInfo vararg export test" {
            val machine = createTestStateMachine(coroutineStarterType, name = "Meta info") {
                // label for state machine
                metaInfo = buildCompositeMetaInfo(
                    buildUmlMetaInfo { umlLabel = "Nested states sm" },
                    object : MetaInfo {} // just a stub
                )
                initialState("State1")
            }
            machine.exportToPlantUml() shouldBe PLANTUML_COMPOSITE_META_INFO
        }

        "CompositeMetaInfo export test" {
            val machine = createTestStateMachine(coroutineStarterType, name = "Meta info") {
                // label for state machine
                metaInfo = buildCompositeMetaInfo {
                    metaInfoSet = setOf(buildUmlMetaInfo { umlLabel = "Nested states sm" })
                }
                initialState("State1")
            }
            machine.exportToPlantUml() shouldBe PLANTUML_COMPOSITE_META_INFO
        }
    }
})