package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ru.nsk.kstatemachine.HistoryType.DEEP
import ru.nsk.kstatemachine.visitors.exportToPlantUml

private const val PLANTUML_NESTED_STATES_RESULT = """@startuml
hide empty description
state State1
state State3
state State2 {
    state Final_subState
    state Initial_subState
    
    [*] --> Initial_subState
    Initial_subState --> Final_subState
    Final_subState --> [*]
}

[*] --> State1
State1 --> State2 : to State2
State1 --> State1
State2 --> State3
State2 --> State1 : back
State3 --> [*]
@enduml
"""

private const val PLANTUML_PARALLEL_STATES_RESULT = """@startuml
hide empty description
state parallel_states {
    state State1 {
        state State11
        state State12
        
        [*] --> State11
        State11 --> State12
        State12 --> State11
    }
    --
    state State2 {
        state State21
        state State22
        
        [*] --> State21
        State21 --> State22
        State22 --> State21
    }
    
}

[*] --> parallel_states
@enduml
"""

private const val PLANTUML_PSEUDO_STATES_RESULT = """@startuml
hide empty description
state state1
state state2 {
    state state21 {
        state state211
        
        [*] --> state211
    }
    state state22
    
    [*] --> state21
}
state state3
state choice <<choice>>
state final

[*] --> state1
final --> [*]
state3 --> state2[H]
state3 --> state2[H*]
@enduml
"""

private const val PLANTUML_COMPOSED_MACHINES_RESULT = """@startuml
hide empty description
state outer_state1
state inner_machine_StateMachine

[*] --> outer_state1
@enduml
"""

class ExportToPlantUmlTest : StringSpec({
    "export nested states" {
        val machine = createStateMachine("Nested states") {
            val state1 = initialState("State1")
            val state3 = finalState("State3")

            val state2 = state("State2") {
                transition<SwitchEvent> { targetState = state3 }
                transition<SwitchEvent>("back") { targetState = state1 }

                val finalSubState = finalState("Final subState")
                initialState("Initial subState") {
                    transition<SwitchEvent> { targetState = finalSubState }
                }
            }

            state1 {
                transition<SwitchEvent>("to ${state2.name}") { targetState = state2 }
                transition<SwitchEvent> { targetState = this@state1 }
                transition<SwitchEvent>()
            }
        }

        machine.exportToPlantUml() shouldBe PLANTUML_NESTED_STATES_RESULT
    }

    "export parallel states" {
        val machine = createStateMachine("Parallel states") {
            initialState("parallel states", ChildMode.PARALLEL) {
                state("State1") {
                    val state11 = initialState("State11")
                    val state12 = state("State12")

                    state11 {
                        transition<SwitchEvent> { targetState = state12 }
                    }
                    state12 {
                        transition<SwitchEvent> { targetState = state11 }
                    }
                }
                state("State2") {
                    val state21 = initialState("State21")
                    val state22 = state("State22")

                    state21 {
                        transition<SwitchEvent> { targetState = state22 }
                    }
                    state22 {
                        transition<SwitchEvent> { targetState = state21 }
                    }
                }
            }
        }

        machine.exportToPlantUml() shouldBe PLANTUML_PARALLEL_STATES_RESULT
    }

    "export with pseudo states" {
        val machine = createStateMachine(enableUndo = true) {
            val state1 = initialState("state1")

            val state2 = state("state2") {
                initialState("state21") {
                    initialState("state211")
                }
                state("state22")
            }
            val shallowHistory = state2.historyState("shallow history")
            val deepHistory = state2.historyState("deep history", historyType = DEEP)

            state("state3") {
                transition<FirstEvent>(targetState = shallowHistory)
                transition<SecondEvent>(targetState = deepHistory)
            }
            choiceState("choice") { state1 }
            finalState("final")
        }

        machine.exportToPlantUml() shouldBe PLANTUML_PSEUDO_STATES_RESULT
    }

    "export composed machines" {
        val inner = createStateMachine("inner machine") {
            initialState("inner state1")
            state("inner state2")
        }
        val outer = createStateMachine("outer machine") {
            initialState("outer state1")
            addState(inner)
        }

        outer.exportToPlantUml() shouldBe PLANTUML_COMPOSED_MACHINES_RESULT
    }
})