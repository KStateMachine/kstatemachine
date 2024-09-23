/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

@file:Suppress("SuspendFunctionOnCoroutineScope")

package ru.nsk.samples

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.persistence.RecordedEvents
import ru.nsk.kstatemachine.persistence.restoreByRecordedEvents
import ru.nsk.kstatemachine.serialization.persistence.RecordedEventsSerializer
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.buildCreationArguments
import ru.nsk.kstatemachine.statemachine.buildEventRecordingArguments
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.samples.SerializationEventRecordingSample.Event1
import ru.nsk.samples.SerializationEventRecordingSample.Event2

private object SerializationEventRecordingSample {
    @Serializable
    class Event1(val data: Int) : Event

    @Serializable
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

private suspend fun CoroutineScope.persistStep(jsonFormat: Json): String {
    val originalMachine = createMachine()

    originalMachine.processEvent(Event1(42))
    originalMachine.processEvent(Event2("text"))

    val recordedEvents = originalMachine.eventRecorder.getRecordedEvents()

    return jsonFormat.encodeToString(recordedEvents)
}

private suspend fun CoroutineScope.restoreStep(jsonFormat: Json, recordedEventsJson: String): StateMachine {
    // use special serializer from kstatemachine-serialization artifact
    val recordedEvents = jsonFormat.decodeFromString<RecordedEvents>(recordedEventsJson)

    val restoredMachine = createMachine()
    restoredMachine.restoreByRecordedEvents(recordedEvents)
    return restoredMachine
}

/**
 * The sample shows how original state machine's state is recorded into [RecordedEvents] object
 * and another machine instance restored from it. And how [RecordedEvents] can be serialized using
 * `kotlinx.serialization` library.
 */
fun main(): Unit = runBlocking {
    val jsonFormat = Json {
        serializersModule = SerializersModule {
            ignoreUnknownKeys = true
            // register library provided serializer for RecordedEvents and its internals
            contextual(RecordedEventsSerializer)

            polymorphic(Event::class) {
                subclass(Event1::class)
                subclass(Event2::class)
            }
            polymorphic(Any::class)
        }
    }

    val recordedEventsJson = persistStep(jsonFormat)
    println(recordedEventsJson)

    // assume we need to restore our machine some time later
    val restoredMachine = restoreStep(jsonFormat, recordedEventsJson)

    check(restoredMachine.activeStates().single().name == "State3")
}