/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package ru.nsk.kstatemachine.serialization.persistence

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.event.SerializableGeneratedEvent
import ru.nsk.kstatemachine.persistence.Record
import ru.nsk.kstatemachine.persistence.RecordedEvents
import ru.nsk.kstatemachine.statemachine.ProcessingResult
import ru.nsk.kstatemachine.transition.EventAndArgument

val KStateMachineSerializersModule = SerializersModule {
    contextual(RecordedEventsSerializer)
    polymorphic(Event::class) {
        subclass(SerializableGeneratedEvent::class, SerializableGeneratedEventSerializer)
    }
}

private object RecordedEventsSerializer : KSerializer<RecordedEvents> {
    override val descriptor = buildClassSerialDescriptor("ru.nsk.kstatemachine.persistence.RecordedEvents") {
        element<Int>("structureHashCode")
        element("records", ListSerializer(RecordSerializer).descriptor)
    }

    override fun serialize(encoder: Encoder, value: RecordedEvents) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.structureHashCode)
            encodeSerializableElement(descriptor, 1, ListSerializer(RecordSerializer), value.records)
        }
    }

    override fun deserialize(decoder: Decoder): RecordedEvents {
        return decoder.decodeStructure(descriptor) {
            if (decodeSequentially()) { // sequential decoding protocol
                RecordedEvents(
                    structureHashCode = decodeIntElement(descriptor, 0),
                    decodeSerializableElement(descriptor, 1, ListSerializer(RecordSerializer)),
                )
            } else {
                var structureHashCode = 0
                var records = emptyList<Record>()
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> structureHashCode = decodeIntElement(descriptor, 0)
                        1 -> records = decodeSerializableElement(descriptor, 1, ListSerializer(RecordSerializer))
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                RecordedEvents(structureHashCode, records)
            }
        }
    }
}

private object RecordSerializer : KSerializer<Record> {
    override val descriptor = buildClassSerialDescriptor("ru.nsk.kstatemachine.persistence.Record") {
        element("eventAndArgument", EventAndArgumentSerializer.descriptor)
        element<ProcessingResult>("processingResult")
    }

    override fun serialize(encoder: Encoder, value: Record) {
        encoder.encodeStructure(RecordedEventsSerializer.descriptor) {
            encodeSerializableElement(descriptor, 0, EventAndArgumentSerializer, value.eventAndArgument)
            encodeSerializableElement(descriptor, 1, serializer<ProcessingResult>(), value.processingResult)
        }
    }

    override fun deserialize(decoder: Decoder): Record {
        return decoder.decodeStructure(descriptor) {
            if (decodeSequentially()) {
                Record(
                    decodeSerializableElement(descriptor, 0, EventAndArgumentSerializer),
                    decodeSerializableElement(descriptor, 1, serializer<ProcessingResult>()),
                )
            } else {
                var eventAndArgument =
                    makeNullPointerFailure<EventAndArgument<*>>("required eventAndArgument property is absent")
                var processingResult =
                    makeNullPointerFailure<ProcessingResult>("required processingResult property is absent")
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> eventAndArgument = Result.success(
                            decodeSerializableElement(descriptor, 0, EventAndArgumentSerializer)
                        )
                        1 -> processingResult = Result.success(
                            decodeSerializableElement(descriptor, 1, serializer<ProcessingResult>())
                        )
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                Record(eventAndArgument.getOrThrow(), processingResult.getOrThrow())
            }
        }
    }
}

private object EventAndArgumentSerializer : KSerializer<EventAndArgument<*>> {
    override val descriptor = buildClassSerialDescriptor("ru.nsk.kstatemachine.transition.EventAndArgument") {
        element("event", PolymorphicSerializer(Event::class).descriptor)
        element("argument", PolymorphicSerializer(Any::class).descriptor, isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: EventAndArgument<*>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, serializer<Event>(), value.event)
            encodeNullableSerializableElement(descriptor, 1, PolymorphicSerializer(Any::class), value.argument)
        }
    }

    override fun deserialize(decoder: Decoder): EventAndArgument<*> {
        return decoder.decodeStructure(descriptor) {
            if (decodeSequentially()) {
                EventAndArgument(
                    decodeSerializableElement(descriptor, 0, serializer<Event>()),
                    decodeNullableSerializableElement(descriptor, 1, PolymorphicSerializer(Any::class)),
                )
            } else {
                var event = makeNullPointerFailure<Event>("required event property is absent")
                var argument: Any? = null
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> event = Result.success(decodeSerializableElement(descriptor, 0, serializer<Event>()))
                        1 -> argument =
                            decodeNullableSerializableElement(descriptor, 1, PolymorphicSerializer(Any::class))
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                EventAndArgument(event.getOrThrow(), argument)
            }
        }
    }
}

private object SerializableGeneratedEventSerializer : KSerializer<SerializableGeneratedEvent> {
    override val descriptor = buildClassSerialDescriptor("ru.nsk.kstatemachine.event.SerializableGeneratedEvent") {
        element<ProcessingResult>("eventType")
    }

    override fun serialize(encoder: Encoder, value: SerializableGeneratedEvent) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, serializer(), value.eventType)
        }
    }

    override fun deserialize(decoder: Decoder): SerializableGeneratedEvent {
        return decoder.decodeStructure(descriptor) {
            if (decodeSequentially()) {
                SerializableGeneratedEvent(decodeSerializableElement(descriptor, 0, serializer()))
            } else {
                var eventType =
                    makeNullPointerFailure<SerializableGeneratedEvent.EventType>("required eventType property is absent")
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> eventType = Result.success(decodeSerializableElement(descriptor, 0, serializer()))
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                SerializableGeneratedEvent(eventType.getOrThrow())
            }
        }
    }
}

private fun <T : Any> makeNullPointerFailure(message: String): Result<T> = Result.failure(NullPointerException(message))