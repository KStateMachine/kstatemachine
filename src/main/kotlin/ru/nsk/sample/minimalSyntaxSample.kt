package ru.nsk.sample

import ru.nsk.kstatemachine.*

// define your events
object SwitchGreenEvent1 : Event
object SwitchYellowEvent1 : Event
object SwitchRedEvent1 : Event

fun main() {
    // setup state machine
    val stateMachine = createStateMachine {
        // setup states and transitions
        val greenState = state("Green")
        val yellowState = state()
        val redState = state {
            transition<SwitchGreenEvent1> { targetState = greenState }
        }

        // configure states
        greenState {
            onEntry { println("Enter $name state") }
            onExit { println("Exit $name state") }
            transition<SwitchYellowEvent1> { targetState = yellowState }
        }

        yellowState {
            transition<SwitchRedEvent1> {
                targetState = redState
                onTriggered { println("Transition on ${it.event}") }
            }
        }
        setInitialState(greenState)
    }

    // process events
    stateMachine.processEvent(SwitchYellowEvent1)
    stateMachine.processEvent(SwitchRedEvent1)
    stateMachine.processEvent(SwitchGreenEvent1)
}