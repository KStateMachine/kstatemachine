/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.serialization.persistence

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import ru.nsk.kstatemachine.event.DataEvent
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.persistence.RecordedEvents
import ru.nsk.kstatemachine.persistence.restoreByRecordedEvents
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.buildCreationArguments
import ru.nsk.kstatemachine.statemachine.buildEventRecordingArguments
import ru.nsk.kstatemachine.statemachine.createStateMachine

@Serializable
private class Event1(val data: Int) : Event

@Serializable
private class Event2(val data: String) : Event

@Serializable
private class IntData(val value: Int)

@Serializable
private class IntDataEvent(override val data: IntData) : DataEvent<IntData>

/**
 * Primitives cannot be serialized in polymorphic context with default serializers.
 */
private object StringPolymorphicSerializer : KSerializer<String> {

    override val descriptor = buildClassSerialDescriptor("com.sample.kotlin.String") {
            element<String>("value")
        }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value)
        }
    }

    override fun deserialize(decoder: Decoder): String {
        return decoder.decodeStructure(descriptor) {
            var value = Result.failure<String>(NullPointerException("value is absent"))
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> value = Result.success(decodeStringElement(descriptor, 0))
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            value.getOrThrow()
        }
    }
}

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
                polymorphic(Any::class) {
                    subclass(String::class, StringPolymorphicSerializer) // for arg
                }
            }
        }

        val originalMachine = createMachine()
        originalMachine.processEvent(Event1(42))
        originalMachine.processEvent(Event2("text"), "arg")
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

    "Serialize and restore state machine with DataTransition" {
        suspend fun CoroutineScope.createMachine(): StateMachine {
            lateinit var state2: DataState<IntData>
            return createStateMachine(
                this,
                "Event recording",
                creationArguments = buildCreationArguments {
                    eventRecordingArguments = buildEventRecordingArguments { }
                }
            ) {
                logger = StateMachine.Logger { println(it()) }
                initialState("State1") {
                    dataTransitionOn<IntDataEvent, IntData> { targetState = { state2 } }
                }
                state2 = dataState("State2")
            }
        }

        val jsonFormat = Json {
            serializersModule = KStateMachineSerializersModule + SerializersModule {
                polymorphic(Event::class) {
                    subclass(IntDataEvent::class)
                }
            }
        }

        val originalMachine = createMachine()
        originalMachine.processEvent(IntDataEvent(IntData(42)))
        val recordedEvents = originalMachine.eventRecorder.getRecordedEvents()
        val recordedEventsJson = jsonFormat.encodeToString(recordedEvents)
        println(recordedEventsJson)

        // restore the machine
        val restoredRecordedEvents = jsonFormat.decodeFromString<RecordedEvents>(recordedEventsJson)

        val restoredMachine = createMachine()
        restoredMachine.restoreByRecordedEvents(restoredRecordedEvents)

        restoredMachine.activeStates() shouldHaveSize 1
        val state = restoredMachine.activeStates().single()
        state.name shouldBe "State2"
        state.shouldBeInstanceOf<DataState<*>>()
        val data = state.data
        data.shouldBeInstanceOf<IntData>()
        data.value shouldBe 42
    }
})