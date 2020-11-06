package ru.nsk.sample

import ru.nsk.kstatemachine.*

// define your events
object TurnOn : Event
object TurnOff : Event

fun main() {
    // setup state machine
    val stateMachine = createStateMachine {
        // setup states and transitions
        val offState = state()
        val onState = state {
            transition<TurnOff> { targetState = offState }
        }
        setInitialState(offState)

        offState {
            transition<TurnOn> { targetState = onState }
        }
    }

    // process events
    stateMachine.processEvent(TurnOn)
    stateMachine.processEvent(TurnOff)
}