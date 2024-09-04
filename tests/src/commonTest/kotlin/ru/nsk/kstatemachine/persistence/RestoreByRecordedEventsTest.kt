/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.persistence

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.called
import io.mockk.verifySequence
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.*
import ru.nsk.kstatemachine.statemachine.StateMachine.*

class RestoreByRecordedEventsTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "negative check ${StateMachine::restoreByRecordedEvents.name} on destroyed machine throws" {
            val recordedEvents = RecordedEvents(0, emptyList())

            val machine = createTestStateMachine(coroutineStarterType) {
                initialState()
            }
            machine.destroy(stop = false)

            shouldThrowWithMessage<IllegalStateException>("$machine is already destroyed") {
                machine.restoreByRecordedEvents(recordedEvents)
            }
        }

        "negative check ${StateMachine::restoreByRecordedEvents.name} on machine that already processed events throws" {
            val recordedEvents = RecordedEvents(0, emptyList())

            val machine = createTestStateMachine(coroutineStarterType) {
                initialState()
                transition<SwitchEvent>()
            }
            machine.processEvent(SwitchEvent)

            shouldThrowWithMessage<IllegalStateException>(
                "$machine has already processed events, ${StateMachine::restoreByRecordedEvents.name}() operation only makes " +
                        "sense on initially clear ${StateMachine::class.simpleName}, please call it before " +
                        "processing any other events (or even before start - optionally)"
            ) { machine.restoreByRecordedEvents(recordedEvents) }
        }

        "check event restoration on different machines without structure check" {
            val machine1 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                initialState()
            }
            val recordedEvents = machine1.eventRecorder.getRecordedEvents()

            val machine2 = createTestStateMachine(coroutineStarterType) {
                initialState()
            }
            shouldNotThrowAny {
                machine2.restoreByRecordedEventsBlocking(recordedEvents, disableStructureHashCodeCheck = true)
            }
        }

        "negative check event restoration on different machines throws" {
            val machine1 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                initialState()
            }
            val recordedEvents = machine1.eventRecorder.getRecordedEvents()

            val machine2 = createTestStateMachine(coroutineStarterType) {
                initialState()
            }
            shouldThrowWithMessage<IllegalStateException>(
                "$machine2 structure seems to be different from recorded original one, you can disable this " +
                        "error by the disableStructureHashCodeCheck argument if you are sure that it is correct"
            ) {
                machine2.restoreByRecordedEventsBlocking(recordedEvents)
            }
        }

        "check event recording preconditions with structure check" {
            val machine1 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                initialState()
            }
            val recordedEvents = machine1.eventRecorder.getRecordedEvents()

            val machine2 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                initialState()
            }
            shouldNotThrowAny { machine2.restoreByRecordedEventsBlocking(recordedEvents) }
        }

        "restore machine with muted callbacks" {
            val callbacks = mockkCallbacks()

            val machine1 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                initialState()
                transition<SwitchEvent>()
            }
            machine1.processEvent(SwitchEvent)
            val recordedEvents = machine1.eventRecorder.getRecordedEvents()

            val machine2 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                initialState()
                transition<SwitchEvent> {
                    callbacks.listen(this)
                }
            }
            machine2.restoreByRecordedEvents(recordedEvents)
            verifySequence {
                callbacks wasNot called
            }
        }

        "restore machine with not muted callbacks" {
            val callbacks = mockkCallbacks()

            val machine1 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                initialState()
                transition<SwitchEvent>()
            }
            machine1.processEvent(SwitchEvent)
            val recordedEvents = machine1.eventRecorder.getRecordedEvents()

            val machine2 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                initialState()
                transition<SwitchEvent> {
                    callbacks.listen(this)
                }
            }
            machine2.restoreByRecordedEvents(recordedEvents, muteListeners = false)
            verifySequence {
                callbacks.onTransitionTriggered(SwitchEvent)
            }
        }

        "restore the machine that is using Undo feature" {
            val machine1 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments {
                    isUndoEnabled = true
                    eventRecordingArguments = buildEventRecordingArguments {}
                }
            ) {
                initialState("first")
                val secondState = state("second")
                transition<SwitchEvent>(targetState = secondState)
            }
            machine1.processEvent(SwitchEvent)
            machine1.undo()
            val recordedEvents = machine1.eventRecorder.getRecordedEvents()

            lateinit var firstState: State
            val machine2 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments {
                    isUndoEnabled = true
                    eventRecordingArguments = buildEventRecordingArguments {}
                }
            ) {
                firstState = initialState("first")
                val secondState = state("second")
                transition<SwitchEvent>(targetState = secondState)
            }
            machine2.restoreByRecordedEvents(recordedEvents)
            machine2.activeStates().shouldContainExactly(firstState)
        }

        "negative restore the machine started with argument" {
            val machine1 = createTestStateMachine(
                coroutineStarterType,
                start = false,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                val firstState = state("first")
                val secondState = state("second")
                initialChoiceState {
                    if (argument == 42) firstState else secondState
                }
            }
            machine1.start(42)
            machine1.processEvent(SwitchEvent)
            val recordedEvents = machine1.eventRecorder.getRecordedEvents()

            lateinit var firstState: State
            val machine2 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                firstState = state("first")
                val secondState = state("second")
                initialChoiceState {
                    if (argument == 42) firstState else secondState
                }
            }
            shouldThrowWithMessage<IllegalStateException>(
                "The ${StateMachine::class.simpleName} is already started, but " +
                        "the ${RecordedEvents::class.simpleName} contains an argument for " +
                        "${StateMachine::start.name} method. " +
                        "To restore such machine, " +
                        "do not start it before calling ${StateMachine::restoreByRecordedEvents.name}"
            ) { machine2.restoreByRecordedEvents(recordedEvents) }
            machine2.isRunning shouldBe true
            machine2.isDestroyed shouldBe false
        }

        "restore the machine started with argument" {
            val machine1 = createTestStateMachine(
                coroutineStarterType,
                name = "machine1",
                start = false,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                val firstState = state("first1")
                val secondState = state("second1")
                initialChoiceState {
                    if (argument == 42) firstState else secondState
                }
            }
            machine1.start(42)
            machine1.processEvent(SwitchEvent)
            val recordedEvents = machine1.eventRecorder.getRecordedEvents()

            lateinit var firstState: State
            val machine2 = createTestStateMachine(
                coroutineStarterType,
                name = "machine2",
                start = false,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                firstState = state("first2")
                val secondState = state("second2")
                initialChoiceState {
                    if (argument == 42) firstState else secondState
                }
            }
            machine2.restoreByRecordedEvents(recordedEvents, disableStructureHashCodeCheck = true)
            machine2.activeStates().shouldContainExactly(firstState)
        }

        "restore the machine with multiple starts" {
            lateinit var secondState: State
            val machine1 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments {
                    eventRecordingArguments = buildEventRecordingArguments { clearRecordsOnMachineRestart = false }
                }
            ) {
                val firstState = state("first")
                secondState = state("second")
                initialChoiceState {
                    if (argument != 42) firstState else secondState
                }
            }
            machine1.stop()
            machine1.start(42)
            machine1.activeStates().shouldContainExactly(secondState)
            val recordedEvents = machine1.eventRecorder.getRecordedEvents()

            val machine2 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments {
                    eventRecordingArguments = buildEventRecordingArguments { clearRecordsOnMachineRestart = false }
                }
            ) {
                val firstState = state("first")
                secondState = state("second")
                initialChoiceState {
                    if (argument != 42) firstState else secondState
                }
            }
            machine2.restoreByRecordedEvents(recordedEvents)
        }
    }
})
