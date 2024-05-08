package ru.nsk.kstatemachine.persistence

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.called
import io.mockk.verifySequence
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.StateMachine.*
import ru.nsk.kstatemachine.statemachine.destroy

class RestoreByRecordedEventsTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "negative check ${StateMachine::restoreRunningMachineByRecordedEvents.name} on not running machine throws" {
            val recordedEvents = RecordedEvents(0, emptyList())

            val machine = createTestStateMachine(coroutineStarterType, start = false) {
                initialState()
                shouldThrowWithMessage<IllegalStateException>(
                    "$machine is not running, ${StateMachine::restoreRunningMachineByRecordedEvents.name}() " +
                            "operation only makes sense on created and started ${StateMachine::class.simpleName}, " +
                            "please call it after the machine is started"
                ) { restoreRunningMachineByRecordedEvents(recordedEvents) }
            }
            val message = "$machine is not running, ${StateMachine::restoreRunningMachineByRecordedEvents.name}() " +
                    "operation only makes sense on created and started ${StateMachine::class.simpleName}, " +
                    "please call it after the machine is started"
            shouldThrowWithMessage<IllegalStateException>(message) {
                machine.restoreRunningMachineByRecordedEvents(recordedEvents)
            }
            shouldThrowWithMessage<IllegalStateException>(message) {
                machine.restoreRunningMachineByRecordedEventsBlocking(recordedEvents)
            }
        }

        "negative check ${StateMachine::restoreRunningMachineByRecordedEvents.name} on destroyed machine throws" {
            val recordedEvents = RecordedEvents(0, emptyList())

            val machine = createTestStateMachine(coroutineStarterType) {
                initialState()
            }
            machine.destroy(stop = false)

            shouldThrowWithMessage<IllegalStateException>("$machine is already destroyed") {
                machine.restoreRunningMachineByRecordedEvents(recordedEvents)
            }
        }

        "negative check ${StateMachine::restoreRunningMachineByRecordedEvents.name} on machine that already processed events throws" {
            val recordedEvents = RecordedEvents(0, emptyList())

            val machine = createTestStateMachine(coroutineStarterType) {
                initialState()
                transition<SwitchEvent>()
            }
            machine.processEvent(SwitchEvent)

            shouldThrowWithMessage<IllegalStateException>(
                "$machine has already processed events, ${StateMachine::restoreRunningMachineByRecordedEvents.name}() " +
                        "operation only makes sense on initially clear ${StateMachine::class.simpleName}, please call it before " +
                        "processing any other events"
            ) { machine.restoreRunningMachineByRecordedEvents(recordedEvents) }
        }

        "check event restoration on different machines without structure check" {
            val machine1 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
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
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
            ) {
                initialState()
            }
            val recordedEvents = machine1.eventRecorder.getRecordedEvents()

            val machine2 = createTestStateMachine(coroutineStarterType) {
                initialState()
            }
            shouldThrow<IllegalStateException> {
                machine2.restoreByRecordedEventsBlocking(recordedEvents)
            }
        }

        "check event recording preconditions with structure check" {
            val machine1 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
            ) {
                initialState()
            }
            val recordedEvents = machine1.eventRecorder.getRecordedEvents()

            val machine2 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
            ) {
                initialState()
            }
            shouldNotThrowAny { machine2.restoreByRecordedEventsBlocking(recordedEvents) }
        }

        "restore machine with muted callbacks" {
            val callbacks = mockkCallbacks()

            val machine1 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
            ) {
                initialState()
                transition<SwitchEvent>()
            }
            machine1.processEvent(SwitchEvent)
            val recordedEvents = machine1.eventRecorder.getRecordedEvents()

            val machine2 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
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
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
            ) {
                initialState()
                transition<SwitchEvent>()
            }
            machine1.processEvent(SwitchEvent)
            val recordedEvents = machine1.eventRecorder.getRecordedEvents()

            val machine2 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
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

        "restore the machine that is not running yet (processes all events as pending)" {
            val machine1 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
            ) {
                initialState()
                val state = state()
                transition<SwitchEvent>(targetState = state)
            }
            machine1.processEvent(SwitchEvent)
            val recordedEvents = machine1.eventRecorder.getRecordedEvents()

            lateinit var state: State
            val machine2 = createTestStateMachine(
                coroutineStarterType,
                start = false,
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
            ) {
                initialState()
                state = state()
                transition<SwitchEvent>(targetState = state)
            }
            machine2.restoreByRecordedEvents(recordedEvents, muteListeners = false)
            machine2.start()
            machine2.activeStates().shouldContainExactly(state)
        }

        "restore the machine that is not running yet with non queued PendingEventHandler (processes all events as pending)" {
            val machine1 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
            ) {
                pendingEventHandler = PendingEventHandler {}
                initialState()
                val state = state()
                transition<SwitchEvent>(targetState = state)
            }
            machine1.processEvent(SwitchEvent)
            val recordedEvents = machine1.eventRecorder.getRecordedEvents()

            lateinit var state: State
            val machine2 = createTestStateMachine(
                coroutineStarterType,
                start = false,
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
            ) {
                pendingEventHandler = PendingEventHandler {}
                initialState()
                state = state()
                transition<SwitchEvent>(targetState = state)
            }
            machine2.restoreByRecordedEvents(recordedEvents, muteListeners = false)
            val exception = shouldThrow<RestorationResultValidationException> {
                machine2.start()
            }
            exception.result.results.single().warnings.single().warningType shouldBe WarningType.PendingEventMightBeIgnored
        }

        "restore the machine that is not running yet with default QueuedPendingEventHandler (processes all events as pending)" {
            val machine1 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
            ) {
                initialState()
                val state = state()
                transition<SwitchEvent>(targetState = state)
            }
            machine1.processEvent(SwitchEvent)
            val recordedEvents = machine1.eventRecorder.getRecordedEvents()

            lateinit var state: State
            val machine2 = createTestStateMachine(
                coroutineStarterType,
                start = false,
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
            ) {
                initialState()
                state = state()
                transition<SwitchEvent>(targetState = state)
            }
            machine2.restoreByRecordedEvents(recordedEvents, muteListeners = false)
            machine2.start()
            machine2.activeStates().shouldContainExactly(state)
        }
    }
})
