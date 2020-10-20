import ru.nsk.kstatemachine.*

// define your events
object SwitchGreenEvent : Event
object SwitchYellowEvent : Event
// events often hold some useful data
class SwitchRedEvent(val data: String) : Event

fun main() {
    val stateMachine = createStateMachine(
        "Traffic lights",
        { message -> println(message) } // enable logging optionally
    ) {
        // setup states
        val greenState = state("Green")
        val yellowState = state("Yellow")
        val redState = state("Red")
        setInitialState(greenState)

        greenState {
            // add listeners, which are signaled on entering or exiting from the state
            onEntry { log("Green light is switched on") }
            onExit { log("Green light will be switched off") }
            // setup transition which is triggered on SwitchYellowEvent
            transition<SwitchYellowEvent> {
                targetState = yellowState
                // add listener which is signaled when transition is triggered
                onTriggered { log("Switching to $targetState") }
            }
        }

        yellowState {
            val transition = transition<SwitchRedEvent> {
                targetState = redState
                onTriggered { log("Switching to $targetState, data: ${it.event.data}") }
            }
            transition.onTriggered { /* just another way for adding listeners*/ }
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
                onTriggered { log("Switching to conditional state, argument: ${it.argument}") }
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