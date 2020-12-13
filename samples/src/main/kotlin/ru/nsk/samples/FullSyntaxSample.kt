package ru.nsk.samples

import ru.nsk.kstatemachine.*

// Define events
object SwitchYellowEvent : Event

// Events often hold some useful data
class SwitchRedEvent(val data: String) : Event

// Subclass DefaultState if you need
class YellowState(val data: Int) : DefaultState("Yellow")

fun main() {
    val machine = createStateMachine(
        "Traffic lights" // StateMachine name is optional
    ) {
        // Setup states
        val greenState = initialState("Green") // State name is optional
        // Use state subclasses
        val yellowState = addState(YellowState(42))
        // State machine finishes when enters final state
        val redState = finalState("Red")

        greenState {
            // Add state listeners
            onEntry { println("Enter $name state") }
            onExit { println("Exit $name state") }

            // Setup transition on SwitchYellowEvent
            transition<SwitchYellowEvent> {
                targetState = yellowState
                // Add transition listener and access argument passed to processEvent() function
                onTriggered { println("Switching to $targetState, with argument: ${it.argument}") }
            }

            // Setup guarded transition
            transitionGuarded<SwitchRedEvent> {
                guard = { false } // Never trigger this transition
                targetState = redState
            }
        }

        yellowState {
            // A conditional transition helps to control when it
            // should be triggered and determine its target state
            transitionConditionally<SwitchRedEvent> {
                direction = {
                    // Suppose you have a function
                    // returning some business logic value which may differ
                    fun getCondition() = 0

                    when (getCondition()) {
                        0 -> targetState(redState)
                        1 -> targetState(greenState)
                        2 -> stay()
                        else -> noTransition()
                    }
                }
                // Access data from a State subclass
                onTriggered { println("Switching state with data: ${this@yellowState.data}") }
            }
        }

        // Set Logger to enable internal state machine logging on your platform
        logger = StateMachine.Logger { println(it) }

        // Set custom IgnoredEventHandler
        // for event that does not match any transition,
        // for example to throw exceptions on ignored events
        ignoredEventHandler = StateMachine.IgnoredEventHandler { currentState, event, argument ->
            error("$currentState does not have transition for $event, argument: $argument")
        }

        // Set custom PendingEventHandler which is triggered
        // if processEvent() is called while previous processEvent() is still processing
        pendingEventHandler = StateMachine.PendingEventHandler { pendingEvent, _ ->
            error(
                "$this can not process pending $pendingEvent " +
                        "as event processing is already running. " +
                        "Do not call processEvent() from notification listeners."
            )
        }

        // Listen to state machine start
        onStarted {
            println("$name started")
        }
    }

    // Listeners might be added in or after setup block
    with(machine) {
        onTransition { sourceState, targetState, event, argument ->
            // Listen to all transitions in one place
            // instead of listening to each transition separately
            println("Transition from $sourceState to $targetState on $event with argument: $argument")
        }
        onStateChanged { state ->
            println("State changed to $state")
        }
        onFinished {
            println("$name finished")
        }
    }

    // Access state after state machine setup
    val greenState = machine.requireState("Green")
    greenState.onEntry { /* Add state listener */ }

    // Access state transition after state machine setup
    val transitionToYellow = greenState.requireTransition<SwitchYellowEvent>()
    transitionToYellow.onTriggered { /* Add transition listener */ }

    // Process events, passing arguments optionally
    machine.processEvent(SwitchYellowEvent, "Get ready!")
    machine.processEvent(SwitchRedEvent("Stop!"))
}