/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.serialization

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.persistence.RecordedEvents
import ru.nsk.kstatemachine.persistence.restoreByRecordedEvents
import ru.nsk.kstatemachine.serialization.persistence.KStateMachineSerializersModule
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.buildCreationArguments
import ru.nsk.kstatemachine.statemachine.buildEventRecordingArguments
import ru.nsk.kstatemachine.statemachine.createStateMachine

@Serializable
class Event1(val data: Int) : Event

@Serializable
class Event2(val data: String) : Event

class RecordedEventsSerializerTest : StringSpec({
    "Serialize and restore state machine with RecordedEvents" {
        suspend fun CoroutineScope.createMachine(): StateMachine {
            lateinit var state2: State
            lateinit var state3: State
            return createStateMachine(
                this,
                "Event recording",
                creationArguments = buildCreationArguments {
                    eventRecordingArguments = buildEventRecordingArguments { }
                }
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

        val jsonFormat = Json {
            serializersModule = KStateMachineSerializersModule + SerializersModule {
                polymorphic(Event::class) {
                    subclass(Event1::class)
                    subclass(Event2::class)
                }
            }
        }

        val originalMachine = createMachine()
        originalMachine.processEvent(Event1(42))
        originalMachine.processEvent(Event2("text"))
        val recordedEvents = originalMachine.eventRecorder.getRecordedEvents()
        val recordedEventsJson = jsonFormat.encodeToString(recordedEvents)
        println(recordedEventsJson)

        // restore the machine
        val restoredRecordedEvents = jsonFormat.decodeFromString<RecordedEvents>(recordedEventsJson)

        val restoredMachine = createMachine()
        restoredMachine.restoreByRecordedEvents(restoredRecordedEvents)

        restoredMachine.activeStates() shouldHaveSize 1
        restoredMachine.activeStates().single().name shouldBe "State3"
    }
})