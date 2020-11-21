package ru.nsk.sample

import ru.nsk.kstatemachine.*

// define your events
object SwitchGreenEvent : Event
object SwitchYellowEvent : Event

// events often hold some useful data
class SwitchRedEvent(val data: String) : Event

// you can subclass State if you need
class RedState(val data: Int) : State("Red")

fun main() {
    val stateMachine = createStateMachine(
        "Traffic lights" // optional name
    ) {
        // setup simple states
        val greenState = state("Green") // state name is optional
        val yellowState = state("Yellow")
        // or add state subclass
        val redState = addState(RedState(42))
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

        // you can use explicit syntax for adding listeners
        greenState.addListener(object : State.Listener {
            override fun onEntry(transitionParams: TransitionParams<*>) {}
            override fun onExit(transitionParams: TransitionParams<*>) {}
        })

        yellowState {
            val transition = transition<SwitchRedEvent> {
                targetState = redState
                // you can access data from events
                onTriggered { println("Switching to $targetState, data: ${it.event.data}") }
            }
            transition.onTriggered { /* just another way for adding listeners */ }
        }
        yellowState.onEntry { /* just another way for adding listeners*/ }

        redState {
            // a conditional transition helps to control when a transition
            // should be triggered and determine its target state
            transitionConditionally<SwitchGreenEvent> {
                direction = {
                    // suppose you have a function
                    // returning some business logic value which may differ
                    fun getCondition() = 0

                    when (getCondition()) {
                        0 -> targetState(greenState)
                        1 -> targetState(yellowState)
                        2 -> stay()
                        else -> noTransition()
                    }
                }
                // you can access argument passed to processEvent() function
                // and data from state subclass
                onTriggered {
                    println("Switching state with argument: ${it.argument}, and data: ${this@redState.data}")
                }
            }
        }

        onTransition { sourceState, targetState, event, argument ->
            // it is possible to listen all transitions in one place
            // instead of listening each transition separately
        }

        // set logger to enable internal state machine logging on your platform
        logger = StateMachine.Logger { println(it) }

        // it is possible to set custom ignored event handler
        // for event that does not match any transition,
        // for example to throw exceptions on ignored events
        ignoredEventHandler = StateMachine.IgnoredEventHandler { _, _, _ ->

        }

        // you can set custom pending event handler which is triggered
        // if processEvent() is called while previous processEvent() is not complete
        pendingEventHandler = StateMachine.PendingEventHandler { pendingEvent, _ ->
            error(
                "$this can not process pending $pendingEvent " +
                        "as event processing is already running. " +
                        "Do not call processEvent() from notification listeners."
            )
        }
    }
    // add listener after state machine setup
    stateMachine.onTransition { sourceState, targetState, event, argument ->
        println("Transition from $sourceState to $targetState on $event with $argument")
    }
    // watch for state changes
    stateMachine.onStateChanged { state ->
        println("State changed to $state")
    }
    // get state after state machine setup
    val greenState = stateMachine.requireState("Green")
    greenState.onEntry { /* add state listener */ }

    // get transition after state machine setup
    val transitionToYellow = greenState.requireTransition<SwitchYellowEvent>()
    transitionToYellow.onTriggered { /* add transition listener */ }

    // process events
    stateMachine.processEvent(SwitchYellowEvent)
    stateMachine.processEvent(SwitchRedEvent("Stop!"))
    // process event and pass argument, instead of adding nullable property to event class
    stateMachine.processEvent(SwitchGreenEvent, "Go!")
}