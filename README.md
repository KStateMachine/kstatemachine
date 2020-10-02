# KStateMachine
State machine (FSM) implementation in Kotlin.

This library uses nice Kotlin DSL syntax, and supports conditions for transitions.

```kotlin
object SwitchGreenEvent : Event
object SwitchYellowEvent : Event
// events often hold some useful data
class SwitchRedEvent(val data: String) : Event

fun main() {
    val stateMachine = createStateMachine(
        "Traffic lights",
        // { message -> println(message) } // enable logging
    ) {
        // setup states
        val greenState = state("Green")
        val yellowState = state("Yellow")
        val redState = state("Red")
        setInitialState(greenState)

        greenState.apply {
            // add listeners, which are signaled on entering or exiting from the state
            onEntry { println("Green light is switched on") }
            onExit { println("Green light will be switched off") }
            // setup transition which is triggered on SwitchYellowEvent
            transition<SwitchYellowEvent> {
                targetState = yellowState
                // add listener which is signaled when transition is triggered
                onTriggered { println("Switching to $targetState") }
            }
        }

        yellowState.apply {
            transition<SwitchRedEvent> {
                targetState = redState
                onTriggered { println("Switching to $targetState, data: ${it.event.data}") }
            }
        }

        redState.apply {
            transition<SwitchGreenEvent> {
                targetState = greenState
                onTriggered { println("Switching to $targetState, argument: ${it.argument}") }
            }
            transition<SwitchGreenEvent> {
                targetState = greenState
                // condition helps to control when transition should be triggered
                condition = { false }
                onTriggered { println("Never get here") }
            }
        }
    }

    // process events
    stateMachine.processEvent(SwitchYellowEvent)
    stateMachine.processEvent(SwitchRedEvent("Stop!"))
    // process event and pass argument, instead of adding nullable property to event class
    stateMachine.processEvent(SwitchGreenEvent, "Go!")
}
```