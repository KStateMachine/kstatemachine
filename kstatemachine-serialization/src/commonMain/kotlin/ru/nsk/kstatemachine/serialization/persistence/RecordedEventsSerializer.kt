/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package ru.nsk.kstatemachine.serialization.persistence

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.event.SerializableGeneratedEvent
import ru.nsk.kstatemachine.event.SerializableGeneratedEvent.EventType
import ru.nsk.kstatemachine.persistence.Record
import ru.nsk.kstatemachine.persistence.RecordedEvents
import ru.nsk.kstatemachine.statemachine.ProcessingResult
import ru.nsk.kstatemachine.transition.EventAndArgument

val KStateMachineSerializersModule = SerializersModule {
    contextual(RecordedEventsSerializer)
    polymorphic(Event::class) {
        subclass(SerializableGeneratedEvent::class, SerializableGeneratedEventSerializer)
    }

    contextual(SerializableGeneratedEventEventTypeStartSerializer)
    polymorphic(EventType::class) {
        subclass(EventType.Start::class, SerializableGeneratedEventEventTypeStartSerializer)
        subclass(EventType.Stop::class, SerializableGeneratedEventEventTypeStopSerializer)
        subclass(
            EventType.Destroy::class,
            SerializableGeneratedEventEventTypeDestroySerializer
        )
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
            if (decodeSequentially()) {
                RecordedEvents(
                    structureHashCode = decodeIntElement(descriptor, 0),
                    decodeSerializableElement(descriptor, 1, ListSerializer(RecordSerializer)),
                )
            } else {
                var structureHashCode = makeNullPointerFailure<Int>("required structureHashCode property is absent")
                var records = makeNullPointerFailure<List<Record>>("required records property is absent")
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> structureHashCode = Result.success(decodeIntElement(descriptor, 0))
                        1 -> records = Result.success(
                            decodeSerializableElement(descriptor, 1, ListSerializer(RecordSerializer))
                        )
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                RecordedEvents(structureHashCode.getOrThrow(), records.getOrThrow())
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
        element("eventType", PolymorphicSerializer(EventType::class).descriptor)
    }

    override fun serialize(encoder: Encoder, value: SerializableGeneratedEvent) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, PolymorphicSerializer(EventType::class), value.eventType)
        }
    }

    override fun deserialize(decoder: Decoder): SerializableGeneratedEvent {
        return decoder.decodeStructure(descriptor) {
            if (decodeSequentially()) {
                SerializableGeneratedEvent(
                    decodeSerializableElement(descriptor, 0, PolymorphicSerializer(EventType::class))
                )
            } else {
                var eventType = makeNullPointerFailure<EventType>("required eventType property is absent")
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> eventType = Result.success(
                            decodeSerializableElement(descriptor, 0, PolymorphicSerializer(EventType::class))
                        )
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                SerializableGeneratedEvent(eventType.getOrThrow())
            }
        }
    }
}

/**
 * See an issue https://github.com/Kotlin/kotlinx.serialization/issues/2830
 */
private object SerializableGeneratedEventEventTypeStartSerializer : KSerializer<EventType.Start> {
    override val descriptor = buildClassSerialDescriptor(
        "ru.nsk.kstatemachine.event.SerializableGeneratedEvent.EventType.Start",
    ) {
        element("ignore_this_field", Boolean.serializer().nullable.descriptor, isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: EventType.Start) {
        encoder.encodeStructure(descriptor) {
            encodeNullableSerializableElement(descriptor, 0, Boolean.serializer().nullable, null)
        }
    }

    override fun deserialize(decoder: Decoder): EventType.Start {
        return decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> decodeNullableSerializableElement(descriptor, 0, Boolean.serializer().nullable) // ignore
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            EventType.Start
        }
    }
}

/**
 * See an issue https://github.com/Kotlin/kotlinx.serialization/issues/2830
 */
private object SerializableGeneratedEventEventTypeStopSerializer : KSerializer<EventType.Stop> {
    override val descriptor =
        buildClassSerialDescriptor("ru.nsk.kstatemachine.event.SerializableGeneratedEvent.EventType.Stop") {
            element("ignore_this_field", Boolean.serializer().nullable.descriptor, isOptional = true)
        }

    override fun serialize(encoder: Encoder, value: EventType.Stop) {
        encoder.encodeStructure(descriptor) {
            encodeNullableSerializableElement(descriptor, 0, Boolean.serializer().nullable, null)
        }
    }

    override fun deserialize(decoder: Decoder): EventType.Stop {
        return decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> decodeNullableSerializableElement(descriptor, 0, Boolean.serializer().nullable) // ignore
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            EventType.Stop
        }
    }
}

private object SerializableGeneratedEventEventTypeDestroySerializer : KSerializer<EventType.Destroy> {
    override val descriptor =
        buildClassSerialDescriptor("ru.nsk.kstatemachine.event.SerializableGeneratedEvent.EventType.Destroy") {
            element<Boolean>("stop")
        }

    override fun serialize(encoder: Encoder, value: EventType.Destroy) {
        encoder.encodeStructure(descriptor) {
            encodeBooleanElement(descriptor, 0, value.stop)
        }
    }

    override fun deserialize(decoder: Decoder): EventType.Destroy {
        return decoder.decodeStructure(descriptor) {
            if (decodeSequentially()) {
                EventType.Destroy(decodeBooleanElement(descriptor, 0))
            } else {
                var stop = makeNullPointerFailure<Boolean>("required stop property is absent")
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> stop = Result.success(decodeBooleanElement(descriptor, 0))
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                EventType.Destroy(stop.getOrThrow())
            }

        }
    }
}

private fun <T : Any> makeNullPointerFailure(message: String): Result<T> = Result.failure(NullPointerException(message))