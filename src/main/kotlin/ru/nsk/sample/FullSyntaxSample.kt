package ru.nsk.sample

import ru.nsk.kstatemachine.*

// Define events
object SwitchGreenEvent : Event
object SwitchYellowEvent : Event

// Events often hold some useful data
class SwitchRedEvent(val data: String) : Event

// You can subclass State if you need
class RedState(val data: Int) : DefaultState("Red")

fun main() {
    val stateMachine = createStateMachine(
        "Traffic lights" // StateMachine name is optional
    ) {
        // Setup simple states
        val greenState = state("Green") // State name is optional
        val yellowState = state("Yellow")
        // You can use state subclasses
        val redState = addState(RedState(42))
        setInitialState(greenState)

        greenState {
            // Add listeners which are notified on entering or exiting from the state
            onEntry { println("Green light is switched on") }
            onExit { println("Green light will be switched off") }
            // Add transition which is triggered on SwitchYellowEvent
            transition<SwitchYellowEvent> {
                targetState = yellowState
                // Add listener which is notified when transition is triggered
                onTriggered { println("Switching to $targetState") }
            }
        }

        // Explicit syntax for adding listeners
        greenState.addListener(object : State.Listener {
            override fun onEntry(transitionParams: TransitionParams<*>) {}
            override fun onExit(transitionParams: TransitionParams<*>) {}
        })

        yellowState {
            val transition = transition<SwitchRedEvent> {
                targetState = redState
                // It is possible to access data from events
                onTriggered { println("Switching to $targetState, data: ${it.event.data}") }
            }
            transition.onTriggered { /* Just another way for adding listeners */ }
        }
        yellowState.onEntry { /* Just another way for adding listeners*/ }

        redState {
            // A conditional transition helps to control when it
            // should be triggered and determine its target state
            transitionConditionally<SwitchGreenEvent> {
                direction = {
                    // Suppose you have a function
                    // returning some business logic value which may differ
                    fun getCondition() = 0

                    when (getCondition()) {
                        0 -> targetState(greenState)
                        1 -> targetState(yellowState)
                        2 -> stay()
                        else -> noTransition()
                    }
                }
                // It is possible to access argument passed to processEvent() function
                // and data from state subclass
                onTriggered {
                    println("Switching state with argument: ${it.argument}, and data: ${this@redState.data}")
                }
            }
        }

        onTransition { sourceState, targetState, event, argument ->
            // It is possible to listen to all transitions in one place
            // instead of listening to each transition separately
        }

        // Set Logger to enable internal state machine logging on your platform
        logger = StateMachine.Logger { println(it) }

        // Set custom IgnoredEventHandler
        // for event that does not match any transition,
        // for example to throw exceptions on ignored events
        ignoredEventHandler = StateMachine.IgnoredEventHandler { currentState, event, argument ->
            error("$currentState does not have transition for $event with $argument")
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
    // Add listener after state machine setup
    stateMachine.onTransition { sourceState, targetState, event, argument ->
        println("Transition from $sourceState to $targetState on $event with $argument")
    }
    // Listen to state changes
    stateMachine.onStateChanged { state ->
        println("State changed to $state")
    }
    // Access state after state machine setup
    val greenState = stateMachine.requireState("Green")
    greenState.onEntry { /* add state listener */ }

    // Access transition after state machine setup
    val transitionToYellow = greenState.requireTransition<SwitchYellowEvent>()
    transitionToYellow.onTriggered { /* Add transition listener */ }

    // Process events
    stateMachine.processEvent(SwitchYellowEvent)
    stateMachine.processEvent(SwitchRedEvent("Stop!"))
    // Process event and pass argument, instead of adding nullable property to event class
    stateMachine.processEvent(SwitchGreenEvent, "Go!")
}