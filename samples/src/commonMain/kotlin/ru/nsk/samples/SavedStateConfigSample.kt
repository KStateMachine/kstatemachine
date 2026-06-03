/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2026.
 * All rights reserved.
 */

@file:Suppress("SuspendFunctionOnCoroutineScope")

package ru.nsk.samples

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import ru.nsk.kstatemachine.event.DataEvent
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.persistence.SavedStateConfig
import ru.nsk.kstatemachine.persistence.captureSavedStateConfig
import ru.nsk.kstatemachine.persistence.restoreBySavedStateConfig
import ru.nsk.kstatemachine.serialization.persistence.KStateMachineSerializersModule
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.NonBlankNamesRequirement.STATES
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.buildCreationArguments
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.samples.SavedStateConfigSample.SwitchEvent
import ru.nsk.samples.SavedStateConfigSample.LoadEvent

private object SavedStateConfigSample {
    object SwitchEvent : Event
    class LoadEvent(override val data: LoadedData) : DataEvent<LoadedData>

    @Serializable
    data class LoadedData(val id: Int, val payload: String)
}

private suspend fun CoroutineScope.createMachine(start: Boolean = true): StateMachine {
    lateinit var idleState: State
    lateinit var loadedState: DataState<SavedStateConfigSample.LoadedData>
    return createStateMachine(
        this,
        "SavedStateConfig sample",
        start = start,
        creationArguments = buildCreationArguments { requireNonBlankNames = STATES },
    ) {
        logger = StateMachine.Logger { println(it()) }
        idleState = initialState("Idle") {
            dataTransitionOn<LoadEvent, SavedStateConfigSample.LoadedData> { targetState = { loadedState } }
        }
        loadedState = dataState("Loaded") {
            onEntry { println("Loaded: $data") }
            transitionOn<SwitchEvent> { targetState = { idleState } }
        }
    }
}

private suspend fun CoroutineScope.persistStep(jsonFormat: Json): String {
    val machine = createMachine()

    machine.processEvent(LoadEvent(SavedStateConfigSample.LoadedData(42, "hello")))

    val snapshot = machine.captureSavedStateConfig()
    return jsonFormat.encodeToString(snapshot)
}

private suspend fun CoroutineScope.restoreStep(
    jsonFormat: Json,
    snapshotJson: String,
): StateMachine {
    val snapshot = jsonFormat.decodeFromString<SavedStateConfig>(snapshotJson)

    val machine = createMachine(start = false)
    machine.restoreBySavedStateConfig(snapshot)
    return machine
}

/**
 * Shows how to capture the active state configuration into a [SavedStateConfig] snapshot and restore a new machine
 * from it. The snapshot has constant size regardless of machine lifetime, unlike event recording.
 *
 * Users must register serializers for their own DataState value types under [Any] in the [SerializersModule],
 * as shown below with [SavedStateConfigSample.LoadedData].
 */
fun main(): Unit = runBlocking {
    val jsonFormat = Json {
        // KStateMachineSerializersModule provides SavedStateConfigSerializer.
        // Users add polymorphic entries for their own DataState value types under Any::class.
        serializersModule = KStateMachineSerializersModule + SerializersModule {
            polymorphic(Any::class) {
                subclass(SavedStateConfigSample.LoadedData::class)
            }
        }
    }

    val snapshotJson = persistStep(jsonFormat)
    println(snapshotJson)

    val restoredMachine = restoreStep(jsonFormat, snapshotJson)

    // restoredMachine is in the same state as the original
    val activeState = restoredMachine.activeStates().single()
    check(activeState.name == "Loaded") { "Expected Loaded, got ${activeState.name}" }
    val data = (activeState as DataState<*>).data as SavedStateConfigSample.LoadedData
    check(data == SavedStateConfigSample.LoadedData(42, "hello")) { "Data mismatch: $data" }
    println("Restored successfully — active state: ${activeState.name}, data: $data")
}
