package ru.nsk.sample

import ru.nsk.kstatemachine.*

// define your events
private object SwitchGreenEvent : Event
private object SwitchYellowEvent : Event

// events often hold some useful data
private class SwitchRedEvent(val data: String) : Event

// you can subclass State if you need
private class RedState(val data: Int) : State("Red")

fun main() {
    val stateMachine = createStateMachine(
        "Traffic lights", // optional name
        { message -> println(message) } // enable logging optionally
    ) {
        // setup simple states
        val greenState = state("Green")
        val yellowState = state("Yellow")
        // or add state subclass
        val redState = addState(RedState(42))
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

        // you can use explicit syntax for adding listeners
        greenState.addListener(object : State.Listener {
            override fun onEntry(transitionParams: TransitionParams<*>) {}
            override fun onExit(transitionParams: TransitionParams<*>) {}
        })

        yellowState {
            val transition = transition<SwitchRedEvent> {
                targetState = redState
                // you can access data from events
                onTriggered { log("Switching to $targetState, data: ${it.event.data}") }
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
                    log("Switching state with argument: ${it.argument}, and data: $data")
                }
            }
        }

        onTransition { sourceState, targetState, event, argument ->
            // it is possible to listen all transitions in one place
            // instead of listening each transition separately
        }

        ignoredEventHandler = StateMachine.IgnoredEventHandler { _, _, _ ->
            // it is possible to set custom ignored event handler
            // for event that does not match any transition,
            // for example to throw exceptions on ignored events
        }

        pendingEventHandler = StateMachine.PendingEventHandler { pendingEvent, _ ->
            // you can set custom pending event handler which is triggered
            // if processEvent() is called while previous processEvent() is not complete
            error("$this can not process pending $pendingEvent as event processing is already running. " +
                    "Do not call processEvent() from notification listeners.")
        }
    }
    stateMachine.onTransition { _, _, _, _ ->
        /* or add listener after state machine setup */
    }

    // process events
    stateMachine.processEvent(SwitchYellowEvent)
    stateMachine.processEvent(SwitchRedEvent("Stop!"))
    // process event and pass argument, instead of adding nullable property to event class
    stateMachine.processEvent(SwitchGreenEvent, "Go!")
}