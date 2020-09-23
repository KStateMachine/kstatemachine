import ru.nsk.kstatemachine.*

class SwitchOnEvent : Event
// events often hold some useful data
class SwitchOffEvent(val reason: String) : Event

fun main() {
    val switcherStateMachine = createStateMachine("Light switcher") {
        // setup states
        val offState = state("Off")

        val onState = state("On") {
            // setup listeners, which are fired on entering or exiting from the state
            onEntry { println("Enter $this") }
            onExit { println("Exit $this") }

            // set transition which is triggered on SwitchOffEvent
            transition<SwitchOffEvent> {
                targetState = offState
                onTriggered { println("Switching off, reason: ${it.event.reason}") }
            }
        }

        setInitialState(offState)

        offState.apply {
            onEntry { println("Enter $this") }
            onExit { println("Exit $this") }

            transition<SwitchOnEvent> {
                targetState = onState
                onTriggered { println("Switching on, argument is ${it.argument}") }
            }
        }
    }

    // post events
    switcherStateMachine.postEvent(SwitchOnEvent())
    switcherStateMachine.postEvent(SwitchOffEvent("No more lights!"))
    // post event specifying argument, instead of adding nullable property to SwitchOnEvent
    switcherStateMachine.postEvent(SwitchOnEvent(), 42)
}