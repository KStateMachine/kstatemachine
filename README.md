# KStateMachine

![Build with Gradle](https://github.com/nsk90/kstatemachine/workflows/Build%20with%20Gradle/badge.svg)

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

        greenState {
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

        yellowState {
            transition<SwitchRedEvent> {
                targetState = redState
                onTriggered { println("Switching to $targetState, data: ${it.event.data}") }
            }
        }

        redState {
            // a conditional transition helps to control when a transition should be triggered and determine its target state
            transitionConditionally<SwitchGreenEvent> {
                direction = {
                    val someCondition = true
                    if (someCondition)
                        targetState(greenState)
                    else
                        noTransition()
                }
                onTriggered { println("Switching to conditional state, argument: ${it.argument}") }
            }
        }

        onTransition { sourceState, targetState, event, argument ->
            // it is possible to listen all transitions in one place instead of listening each transition separately
        }
    }

    // process events
    stateMachine.processEvent(SwitchYellowEvent)
    stateMachine.processEvent(SwitchRedEvent("Stop!"))
    // process event and pass argument, instead of adding nullable property to event class
    stateMachine.processEvent(SwitchGreenEvent, "Go!")
}
```