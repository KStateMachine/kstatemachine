/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.persistence

import ru.nsk.kstatemachine.VisibleForTesting
import ru.nsk.kstatemachine.state.DataState
import ru.nsk.kstatemachine.state.DefaultDataState
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.state.activeStates
import ru.nsk.kstatemachine.state.requireState
import ru.nsk.kstatemachine.statemachine.InternalStateMachine
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.checkNotDestroyed
import ru.nsk.kstatemachine.testing.Testing.startFrom
import ru.nsk.kstatemachine.visitors.structureHashCode

/**
 * A constant-size snapshot of an active state configuration.
 * Captured with [captureSavedStateConfig] and restored with [restoreBySavedStateConfig].
 *
 * Unlike event recording, the snapshot size is independent of how many events the machine has processed.
 * Restoring via [restoreBySavedStateConfig] genuinely enters states (using the same mechanism as
 * [ru.nsk.kstatemachine.testing.Testing.startFrom]), so listener callbacks fire normally.
 */
class SavedStateConfig @VisibleForTesting constructor(
    val structureHashCode: Int,
    /** Names of the leaf active states at capture time. Passed to startFrom() on restoration. */
    val activeLeafStateNames: List<String>,
    /**
     * Last remembered data value per DataState, keyed by state name.
     * Includes any DataState that had a value at capture time, whether active or not.
     * On restoration the value is fed into the state's internal last-data slot; for active states
     * the entry path then derives [DataState.data] from it via the usual fall-back.
     */
    val dataStateLastValues: Map<String, Any?>,
)

/**
 * Captures the current active state configuration of this machine as a [SavedStateConfig] snapshot.
 *
 * Prerequisites (throws [IllegalStateException] if violated):
 * - Machine must be running and not destroyed.
 * - [StateMachine.creationArguments].isUndoEnabled must be false — the undo stack cannot be persisted.
 *   Pass [disableUndoEnabledCheck] = true to bypass this check; the restored machine will start with
 *   an empty undo stack but can still record and undo events processed after restoration.
 * - All active states must have non-blank names.
 *   Use `requireNonBlankNames` with `STATES` value or `STATES_AND_TRANSITIONS` in creation arguments
 *   to enforce this at machine start, or assign names to all states manually.
 */
fun StateMachine.captureSavedStateConfig(
    disableUndoEnabledCheck: Boolean = false,
): SavedStateConfig {
    checkNotDestroyed()
    check(isRunning) { "$this is not running" }
    if (!disableUndoEnabledCheck)
        check(!creationArguments.isUndoEnabled) {
            "captureSavedStateConfig() is not compatible with isUndoEnabled = true: " +
                    "the undo stack cannot be persisted and would be empty after restoration. " +
                    "Pass disableUndoEnabledCheck = true to opt in: the restored machine will start " +
                    "with an empty undo stack but accept undo for events processed after restoration."
        }

    val active = activeStates()

    val leafStates = active.filter { s -> s.states.none { child -> child in active } }
    leafStates.forEach { state ->
        check(!state.name.isNullOrBlank()) {
            "Active state $state has no name — assign names to all active states " +
                    "or use requireNonBlankNames with STATES of STATES_AND_TRANSITIONS in buildCreationArguments {}"
        }
    }

    val lastValues = mutableMapOf<String, Any?>()

    collectAllDefaultDataStates().forEach { state ->
        val last = state.lastDataOrNull ?: return@forEach
        check(!state.name.isNullOrBlank()) {
            "DataState $state has lastData set but no name — assign names to all DataStates " +
                    "or use requireNonBlankNames with STATES or STATES_AND_TRANSITIONS in buildCreationArguments {}"
        }
        lastValues[state.name!!] = last
    }

    return SavedStateConfig(
        structureHashCode = structureHashCode,
        activeLeafStateNames = leafStates.map { it.name!! },
        dataStateLastValues = lastValues,
    )
}

/**
 * Restores this machine to the state configuration described by [savedStateConfig].
 * Starts the machine if it is not already started.
 *
 * The machine must not have processed any events before restoration (the same restriction as
 * [restoreByRecordedEvents]).
 *
 * Unlike [restoreByRecordedEvents], listener callbacks fire normally during restoration because
 * states are genuinely entered via [ru.nsk.kstatemachine.testing.Testing.startFrom].
 *
 * @param disableStructureHashCodeCheck skip the structural integrity check — useful when intentionally
 *   restoring on a machine with a different structure (results may differ).
 */
suspend fun StateMachine.restoreBySavedStateConfig(
    savedStateConfig: SavedStateConfig,
    disableStructureHashCodeCheck: Boolean = false,
): Unit = coroutineAbstraction.withContext {
    checkNotDestroyed()
    if (isRunning) {
        check(!hasProcessedEvents) {
            "$this has already processed events, ${::restoreBySavedStateConfig.name}() only makes sense on an " +
                    "initially clear ${StateMachine::class.simpleName}"
        }
    }

    if (!disableStructureHashCodeCheck)
        check(structureHashCode == savedStateConfig.structureHashCode) {
            "$this structure seems to be different from the captured original; you can disable this check via " +
                    "disableStructureHashCodeCheck = true if you are sure the machines are compatible"
        }

    savedStateConfig.dataStateLastValues.forEach { (name, lastData) ->
        val state = requireState(name) as DefaultDataState<*>
        state.restoreData(lastData)
    }

    val leafStates = savedStateConfig.activeLeafStateNames.map { requireState(it) }.toSet()
    (this as InternalStateMachine).startFrom(leafStates, argument = null)
}

/**
 * Blocking analog of [restoreBySavedStateConfig].
 */
fun StateMachine.restoreBySavedStateConfigBlocking(
    savedStateConfig: SavedStateConfig,
    disableStructureHashCodeCheck: Boolean = false,
) = coroutineAbstraction.runBlocking {
    restoreBySavedStateConfig(savedStateConfig, disableStructureHashCodeCheck)
}

private fun IState.collectAllDefaultDataStates(): List<DefaultDataState<*>> = buildList {
    for (state in states) {
        if (state is DefaultDataState<*>) add(state)
        if (state !is StateMachine) addAll(state.collectAllDefaultDataStates())
    }
}
