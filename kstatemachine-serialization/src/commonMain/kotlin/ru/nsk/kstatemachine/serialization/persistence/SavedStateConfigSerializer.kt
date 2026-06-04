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
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import ru.nsk.kstatemachine.persistence.SavedStateConfig

internal object SavedStateConfigSerializer : KSerializer<SavedStateConfig> {
    private val anySerializer = PolymorphicSerializer(Any::class)
    private val lastValuesSerializer = MapSerializer(String.serializer(), anySerializer.nullable)

    override val descriptor = buildClassSerialDescriptor(
        "ru.nsk.kstatemachine.persistence.SavedStateConfig"
    ) {
        element<Int>("structureHashCode")
        element("activeLeafStateNames", ListSerializer(String.serializer()).descriptor)
        element("dataStateLastValues", lastValuesSerializer.descriptor)
    }

    override fun serialize(encoder: Encoder, value: SavedStateConfig) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.structureHashCode)
            encodeSerializableElement(descriptor, 1, ListSerializer(String.serializer()), value.activeLeafStateNames)
            encodeSerializableElement(descriptor, 2, lastValuesSerializer, value.dataStateLastValues)
        }
    }

    override fun deserialize(decoder: Decoder): SavedStateConfig {
        return decoder.decodeStructure(descriptor) {
            if (decodeSequentially()) {
                SavedStateConfig(
                    structureHashCode = decodeIntElement(descriptor, 0),
                    activeLeafStateNames = decodeSerializableElement(descriptor, 1, ListSerializer(String.serializer())),
                    dataStateLastValues = decodeSerializableElement(descriptor, 2, lastValuesSerializer),
                )
            } else {
                var structureHashCode = makeNullPointerFailure<Int>("required structureHashCode property is absent")
                var activeLeafStateNames =
                    makeNullPointerFailure<List<String>>("required activeLeafStateNames property is absent")
                var dataStateLastValues =
                    makeNullPointerFailure<Map<String, Any?>>("required dataStateLastValues property is absent")
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> structureHashCode = Result.success(decodeIntElement(descriptor, 0))
                        1 -> activeLeafStateNames = Result.success(
                            decodeSerializableElement(descriptor, 1, ListSerializer(String.serializer()))
                        )
                        2 -> dataStateLastValues = Result.success(
                            decodeSerializableElement(descriptor, 2, lastValuesSerializer)
                        )
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                SavedStateConfig(
                    structureHashCode.getOrThrow(),
                    activeLeafStateNames.getOrThrow(),
                    dataStateLastValues.getOrThrow(),
                )
            }
        }
    }
}

private fun <T : Any> makeNullPointerFailure(message: String): Result<T> =
    Result.failure(NullPointerException(message))
