/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.statemachine

interface CreationArguments {
    /**
     * Allows the library to automatically call destroy() on current state owning machine instance if user tries
     * to reuse its states in another machine. Usually this is a result of using object states in sequentially created
     * similar machines. destroy() will be called on the previous machine instance.
     * If set to false an exception will be thrown on state reuse attempt.
     * Default: true
     */
    val autoDestroyOnStatesReuse: Boolean

    /**
     * Enables Undo transition.
     * Default: false
     */
    val isUndoEnabled: Boolean

    /**
     * If set to true, when multiple transitions match event the first matching transition is selected.
     * if set to false, when multiple transitions match event exception is thrown.
     * Default: false
     */
    val doNotThrowOnMultipleTransitionsMatch: Boolean

    /**
     * If enabled, throws exception on the machine start,
     * if it contains states or transitions with null or blank names
     * Default: false
     */
    val requireNonBlankNames: Boolean

    /**
     * If set, enables incoming events recording in order to restore [StateMachine] later.
     * By default, event recording is disabled.
     * Use [StateMachine.eventRecorder] to access the recording result.
     * Default: null
     */
    val eventRecordingArguments: EventRecordingArguments?
}

interface CreationArgumentsBuilder : CreationArguments {
    override var autoDestroyOnStatesReuse: Boolean
    override var isUndoEnabled: Boolean
    override var doNotThrowOnMultipleTransitionsMatch: Boolean
    override var requireNonBlankNames: Boolean
    override var eventRecordingArguments: EventRecordingArguments?
}

private data class CreationArgumentsBuilderImpl(
    override var autoDestroyOnStatesReuse: Boolean = true,
    override var isUndoEnabled: Boolean = false,
    override var doNotThrowOnMultipleTransitionsMatch: Boolean = false,
    override var requireNonBlankNames: Boolean = false,
    override var eventRecordingArguments: EventRecordingArguments? = null
) : CreationArgumentsBuilder

fun buildCreationArguments(builder: CreationArgumentsBuilder.() -> Unit): CreationArguments =
    CreationArgumentsBuilderImpl().apply(builder).copy()

interface EventRecordingArguments {
    /**
     * If enabled removes all recorded events when detects that the machine was stopped and started again.
     * Default: true
     */
    val clearRecordsOnMachineRestart: Boolean

    /**
     * If enabled skips ignored events, supposing they do not affect restoration of the machine
     * Default: true
     */
    val skipIgnoredEvents: Boolean
}

interface EventRecordingArgumentsBuilder : EventRecordingArguments {
    override var clearRecordsOnMachineRestart: Boolean
    override var skipIgnoredEvents: Boolean
}

private data class EventRecordingArgumentsBuilderImpl(
    override var clearRecordsOnMachineRestart: Boolean = true,
    override var skipIgnoredEvents: Boolean = true,
) : EventRecordingArgumentsBuilder

fun buildEventRecordingArguments(builder: EventRecordingArgumentsBuilder.() -> Unit): EventRecordingArguments =
    EventRecordingArgumentsBuilderImpl().apply(builder).copy()