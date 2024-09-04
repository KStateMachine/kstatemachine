/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.samples

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.persistence.RecordedEvents
import ru.nsk.kstatemachine.persistence.restoreByRecordedEvents
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.buildCreationArguments
import ru.nsk.kstatemachine.statemachine.buildEventRecordingArguments
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.samples.EventRecordingSample.Event1
import ru.nsk.samples.EventRecordingSample.Event2

private object EventRecordingSample {
    class Event1(val data: Int) : Event
    class Event2(val data: String) : Event
}

private suspend fun CoroutineScope.createMachine(): StateMachine {
    lateinit var state2: State
    lateinit var state3: State
    return createStateMachine(
        this,
        "Event recording",
        creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments { } }
    ) {
        logger = StateMachine.Logger { println(it()) }
        initialState("State1") {
            transitionOn<Event1> { targetState = { state2 } }
        }
        state2 = state("State2") {
            transitionOn<Event2> { targetState = { state3 } }
        }
        state3 = state("State3")
    }
}

/**
 * The sample shows how original state machine's state is recorded into [RecordedEvents] object
 * and another machine instance restored from it.
 */
fun main(): Unit = runBlocking {
    val originalMachine = createMachine()

    originalMachine.processEvent(Event1(42))
    originalMachine.processEvent(Event2("text"))

    val recordedEvents = originalMachine.eventRecorder.getRecordedEvents()
    // recordedEvents can be serialized by client code.

    val restoredMachine = createMachine()
    restoredMachine.restoreByRecordedEvents(recordedEvents)

    check(restoredMachine.activeStates().single().name == "State3")
}