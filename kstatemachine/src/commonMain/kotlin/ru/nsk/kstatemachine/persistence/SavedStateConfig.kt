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
import ru.nsk.kstatemachine.statemachine.NonBlankNamesRequirement.STATES
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.checkNotDestroyed
import ru.nsk.kstatemachine.statemachine.use
import ru.nsk.kstatemachine.testing.Testing.startFrom
import ru.nsk.kstatemachine.visitors.checkNonBlankNames
import ru.nsk.kstatemachine.visitors.structureHashCode

/**
 * A constant-size snapshot of an active state configuration.
 * Captured with [captureSavedStateConfig] and restored with [restoreBySavedStateConfig].
 * This type is for data serialization.
 *
 * Unlike event recording, the snapshot size is independent of how many events the machine has processed.
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
 * - All states must have non-blank names.
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

    checkNonBlankNames(STATES)

    val active = activeStates()

    val leafStates = active.filter { s -> s.states.none { child -> child in active } }

    val lastValues = mutableMapOf<String, Any?>()

    collectAllDefaultDataStates().forEach { state ->
        val last = state.lastDataOrNull ?: return@forEach
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
 * Prerequisites (throws [IllegalStateException] if violated):
 * - Machine must not be destroyed.
 * - Machine must not have processed any events (the same restriction as [restoreByRecordedEvents]).
 * - All states must have non-blank names (same requirement as [captureSavedStateConfig]).
 *
 * @param muteListeners by default listener callbacks are suppressed during restoration, since the
 *   original machine already triggered them at the original moment. Pass `false` to receive the
 *   entry/exit/transition callbacks on the restored machine too.
 * @param disableStructureHashCodeCheck skip the structural integrity check — useful when intentionally
 *   restoring on a machine with a different structure (results may differ).
 */
suspend fun StateMachine.restoreBySavedStateConfig(
    savedStateConfig: SavedStateConfig,
    muteListeners: Boolean = true,
    disableStructureHashCodeCheck: Boolean = false,
): Unit = coroutineAbstraction.withContext {
    checkNotDestroyed()
    if (isRunning) {
        check(!hasProcessedEvents) {
            "$this has already processed events, ${::restoreBySavedStateConfig.name}() only makes sense on an " +
                    "initially clear ${StateMachine::class.simpleName}"
        }
    }

    checkNonBlankNames(STATES)

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
    this as InternalStateMachine
    val mutationSection = if (muteListeners) openListenersMutationSection() else EmptyListenersMutationSection
    mutationSection.use {
        startFrom(leafStates, argument = null)
    }
}

/**
 * Blocking analog of [restoreBySavedStateConfig].
 */
fun StateMachine.restoreBySavedStateConfigBlocking(
    savedStateConfig: SavedStateConfig,
    muteListeners: Boolean = true,
    disableStructureHashCodeCheck: Boolean = false,
) = coroutineAbstraction.runBlocking {
    restoreBySavedStateConfig(savedStateConfig, muteListeners, disableStructureHashCodeCheck)
}

private fun IState.collectAllDefaultDataStates(): List<DefaultDataState<*>> = buildList {
    for (state in states) {
        if (state is DefaultDataState<*>) add(state)
        if (state !is StateMachine) addAll(state.collectAllDefaultDataStates())
    }
}
