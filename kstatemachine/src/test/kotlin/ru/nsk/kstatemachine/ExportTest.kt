package ru.nsk.kstatemachine

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import ru.nsk.kstatemachine.visitors.exportToDot
import ru.nsk.kstatemachine.visitors.exportToPlantUml

private const val DOT_RESULT = """digraph state_machine {
    label="Traffic lights";
    Green;
    Red;
    Yellow;

    INITIAL -> Green;
    Green -> DefaultState(name=Yellow) [ label = "to yellow" ];
    Green -> DefaultState(name=Green);
    Yellow -> DefaultFinalState(name=Red);
    Yellow -> DefaultState(name=Green) [ label = "back" ];
    Red -> FINISH;

    INITIAL [ shape = point ];
    FINISH [ shape = point ];
}
"""

private const val PLANTUML_RESULT = """@startuml
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

class ExportTest {
    @Test
    fun exportToDot() {
        val machine = createStateMachine("Traffic lights") {
            val greenState = initialState("Green")
            val redState = finalState("Red")

            val yellowState = state("Yellow") {
                transition<SwitchEvent> { targetState = redState }
                transition<SwitchEvent>("back") { targetState = greenState }
            }

            greenState {
                transition<SwitchEvent>("to yellow") { targetState = yellowState }
                transition<SwitchEvent> { targetState = this@greenState }
                transition<SwitchEvent>()
            }
        }

        assertThat(machine.exportToDot(), equalTo(DOT_RESULT))
    }

    @Test
    fun exportToPlantUml() {
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

        assertThat(machine.exportToPlantUml(), equalTo(PLANTUML_RESULT))
    }
}