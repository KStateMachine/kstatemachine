package ru.nsk.kstatemachine.persistence

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.event.StartEvent
import ru.nsk.kstatemachine.event.UndoEvent
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.statemachine.*
import ru.nsk.kstatemachine.statemachine.ProcessingResult.IGNORED
import ru.nsk.kstatemachine.statemachine.ProcessingResult.PROCESSED
import ru.nsk.kstatemachine.statemachine.StateMachine.CreationArguments
import ru.nsk.kstatemachine.statemachine.StateMachine.EventRecordingArguments
import ru.nsk.kstatemachine.transition.EventAndArgument
import ru.nsk.kstatemachine.visitors.structureHashCode

class EventRecorderTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "negative event recording should be explicitly enabled" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState()
            }
            shouldThrowWithMessage<IllegalStateException>(
                "Event recording is not enabled. Use ${CreationArguments::eventRecordingArguments.name} parameter " +
                        "of createStateMachine() method family, to enable it first"
            ) { machine.eventRecorder }
        }

        "check recorded events with arguments" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
            ) {
                initialState()
                transition<FirstEvent>()
                transition<SecondEvent>()
            }
            machine.processEvent(FirstEvent)
            machine.processEvent(SecondEvent, 2)

            val recordedEvents = machine.eventRecorder.getRecordedEvents()
            recordedEvents.structureHashCode shouldBe machine.structureHashCode

            recordedEvents.records.first().eventAndArgument.event.shouldBeInstanceOf<StartEvent>()
            recordedEvents.records.shouldContainInOrder(
                Record(EventAndArgument(FirstEvent, null), PROCESSED),
                Record(EventAndArgument(SecondEvent, 2), PROCESSED),
            )
        }

        "check recorded events and undo" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(
                    isUndoEnabled = true,
                    eventRecordingArguments = EventRecordingArguments()
                )
            ) {
                initialState()
                transition<SwitchEvent>()
            }
            machine.processEvent(SwitchEvent)
            machine.undo()

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.records.first().eventAndArgument.event.shouldBeInstanceOf<StartEvent>()
            recordedEvents.records.shouldContainInOrder(
                Record(EventAndArgument(SwitchEvent, null), PROCESSED),
                Record(EventAndArgument(UndoEvent, null), PROCESSED),
            )
        }

        "check recorded events with StopEvent" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
            ) {
                initialState()
                transition<SwitchEvent>()
            }
            machine.processEvent(SwitchEvent)
            machine.stop()

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.records.first().eventAndArgument.event.shouldBeInstanceOf<StartEvent>()
            recordedEvents.records.shouldContain(
                Record(EventAndArgument(SwitchEvent, null), PROCESSED),
            )
            recordedEvents.records shouldHaveSize 3 // StopEvent
        }

        "check recorded events with DestroyEvent" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
            ) {
                initialState()
                transition<SwitchEvent>()
            }
            machine.processEvent(SwitchEvent)
            machine.destroy()

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.records.first().eventAndArgument.event.shouldBeInstanceOf<StartEvent>()
            recordedEvents.records.shouldContain(
                Record(EventAndArgument(SwitchEvent, null), PROCESSED),
            )
            recordedEvents.records shouldHaveSize 3 // DestroyEvent
        }

        "check recorded events on restart without ${EventRecordingArguments::clearRecordsOnMachineRestart::name} flag" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(
                    eventRecordingArguments = EventRecordingArguments(
                        clearRecordsOnMachineRestart = false,
                    )
                )
            ) {
                initialState()
                transition<FirstEvent>()
                transition<SecondEvent>()
            }
            machine.processEvent(FirstEvent)
            machine.restart()
            machine.processEvent(SecondEvent)

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.records.first().eventAndArgument.event.shouldBeInstanceOf<StartEvent>()
            recordedEvents.records.shouldContainInOrder(
                Record(EventAndArgument(FirstEvent, null), PROCESSED),
                Record(EventAndArgument(SecondEvent, null), PROCESSED),
            )
            recordedEvents.records shouldHaveSize 5
        }

        "check recorded events on restart with ${EventRecordingArguments::clearRecordsOnMachineRestart::name} flag (default)" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
            ) {
                initialState()
                transition<FirstEvent>()
                transition<SecondEvent>()
            }
            machine.processEvent(FirstEvent)
            machine.restart()
            machine.processEvent(SecondEvent)

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.records.first().eventAndArgument.event.shouldBeInstanceOf<StartEvent>()
            recordedEvents.records.shouldContain(
                Record(EventAndArgument(SecondEvent, null), PROCESSED),
            )
            recordedEvents.records shouldHaveSize 2
        }

        "check recorded events with ${EventRecordingArguments::skipIgnoredEvents::name} flag (default)" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(eventRecordingArguments = EventRecordingArguments())
            ) {
                initialState()
            }
            machine.processEvent(SwitchEvent)

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.records.first().eventAndArgument.event.shouldBeInstanceOf<StartEvent>()
            recordedEvents.records shouldHaveSize 1
        }

        "check recorded events without ${EventRecordingArguments::skipIgnoredEvents::name} flag" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(
                    eventRecordingArguments = EventRecordingArguments(
                        skipIgnoredEvents = false
                    )
                )
            ) {
                initialState()
            }
            machine.processEvent(SwitchEvent)

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.records.first().eventAndArgument.event.shouldBeInstanceOf<StartEvent>()
            recordedEvents.records.shouldContain(
                Record(EventAndArgument(SwitchEvent, null), IGNORED),
            )
            recordedEvents.records shouldHaveSize 2
        }
    }
})
