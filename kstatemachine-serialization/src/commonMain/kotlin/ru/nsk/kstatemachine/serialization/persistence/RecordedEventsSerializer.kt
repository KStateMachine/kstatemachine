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
import kotlinx.serialization.serializer
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.event.StartEvent
import ru.nsk.kstatemachine.event.StartEventImpl
import ru.nsk.kstatemachine.persistence.Record
import ru.nsk.kstatemachine.persistence.RecordedEvents
import ru.nsk.kstatemachine.statemachine.ProcessingResult
import ru.nsk.kstatemachine.transition.EventAndArgument

object RecordedEventsSerializer : KSerializer<RecordedEvents> {
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

object RecordSerializer : KSerializer<Record> {
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

object EventAndArgumentSerializer : KSerializer<EventAndArgument<*>> {
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

// fixme should I serialize generatedEvents??
object StartEventSerializer : KSerializer<StartEvent> {
    override val descriptor = buildClassSerialDescriptor("ru.nsk.kstatemachine.event.StartEvent") {
        element<String>("startState")
    }

    override fun serialize(encoder: Encoder, value: StartEvent) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, requireNotNull(value.startState.name) {
                "StartEvent must have a name"
            })
        }
    }

    override fun deserialize(decoder: Decoder): StartEvent {
        return decoder.decodeStructure(descriptor) {
            if (decodeSequentially()) {
                StartEventImpl(emptySet())
            } else {
                TODO()
            }
        }
    }
}

private fun <T : Any> makeNullPointerFailure(message: String): Result<T> = Result.failure(NullPointerException(message))