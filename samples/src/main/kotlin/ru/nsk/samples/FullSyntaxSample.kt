package ru.nsk.samples

import ru.nsk.kstatemachine.*

// Define events
object SwitchYellowEvent : Event

// Events often hold some useful data
class SwitchRedEvent(val data: String) : Event

// You can subclass State if you need
class YellowState(val data: Int) : DefaultState("Yellow")

fun main() {
    val stateMachine = createStateMachine(
        "Traffic lights" // StateMachine name is optional
    ) {
        // Setup simple states
        val greenState = initialState("Green") // State name is optional
        // You can use state subclasses
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
    }

    // Listen to transition changes in or after state machine setup block
    stateMachine.onTransition { sourceState, targetState, event, argument ->
        // It is possible to listen to all transitions in one place
        // instead of listening to each transition separately
        println("Transition from $sourceState to $targetState on $event with argument: $argument")
    }

    // Listen to state changes in or after state machine setup block
    stateMachine.onStateChanged { state ->
        println("State changed to $state")
    }

    // Listen to state changes in or after state machine setup block
    stateMachine.onFinished {
        println("$name finished")
    }

    // Access state after state machine setup
    val greenState = stateMachine.requireState("Green")
    greenState.onEntry { /* Add state listener */ }

    // Access state transition after state machine setup
    val transitionToYellow = greenState.requireTransition<SwitchYellowEvent>()
    transitionToYellow.onTriggered { /* Add transition listener */ }

    // Process events passing arguments optionally
    stateMachine.processEvent(SwitchYellowEvent, "Get ready!")
    stateMachine.processEvent(SwitchRedEvent("Stop!"))
}