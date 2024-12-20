/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.persistence

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.event.FinishedEvent
import ru.nsk.kstatemachine.event.SerializableGeneratedEvent
import ru.nsk.kstatemachine.event.UndoEvent
import ru.nsk.kstatemachine.event.WrappedEvent
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.*
import ru.nsk.kstatemachine.statemachine.ProcessingResult.IGNORED
import ru.nsk.kstatemachine.statemachine.ProcessingResult.PROCESSED
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

        "negative process SerializableGeneratedEvent" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments {
                    eventRecordingArguments = buildEventRecordingArguments { skipIgnoredEvents = false }
                }
            ) {
                initialState()
            }
            shouldThrowWithMessage<IllegalStateException>("Never get here, SerializableGeneratedEvent should not be processed") {
                machine.processEvent(SerializableGeneratedEvent(SerializableGeneratedEvent.EventType.Start))
            }
        }

        "negative process WrappedEvent" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments {
                    eventRecordingArguments = buildEventRecordingArguments { skipIgnoredEvents = false }
                }
            ) {
                initialState()
            }
            shouldThrowWithMessage<IllegalStateException>("Never get here") {
                machine.processEvent(WrappedEvent(SwitchEvent, argument = null))
            }
        }

        "check recorded events with arguments" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                initialState()
                transition<FirstEvent>()
                transition<SecondEvent>()
            }
            machine.processEvent(FirstEvent)
            machine.processEvent(SecondEvent, 2)

            val recordedEvents = machine.eventRecorder.getRecordedEvents()
            recordedEvents.structureHashCode shouldBe machine.structureHashCode

            recordedEvents.firstEventShouldBeStart()
            recordedEvents.records.shouldContainInOrder(
                Record(EventAndArgument(FirstEvent, null), PROCESSED),
                Record(EventAndArgument(SecondEvent, 2), PROCESSED),
            )
        }

        "check recorded events with start argument" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                start = false,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                val state1 = state("state1")
                val state2 = state("state2")
                initialChoiceState {
                    if (argument != 2) state1 else state2
                }
            }
            machine.start(argument = 2)

            val recordedEvents = machine.eventRecorder.getRecordedEvents()
            recordedEvents.structureHashCode shouldBe machine.structureHashCode

            recordedEvents.records.shouldContainExactly(
                Record(
                    EventAndArgument(SerializableGeneratedEvent(SerializableGeneratedEvent.EventType.Start), 2),
                    PROCESSED
                )
            )
        }

        "check recorded events and undo" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments {
                    isUndoEnabled = true
                    eventRecordingArguments = buildEventRecordingArguments {}
                }
            ) {
                initialState()
                transition<SwitchEvent>()
            }
            machine.processEvent(SwitchEvent)
            machine.undo()

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.firstEventShouldBeStart()
            recordedEvents.records.shouldContainInOrder(
                Record(EventAndArgument(SwitchEvent, null), PROCESSED),
                Record(EventAndArgument(UndoEvent, null), PROCESSED),
            )
        }

        "check recorded events with StopEvent" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                initialState()
                transition<SwitchEvent>()
            }
            machine.processEvent(SwitchEvent)
            machine.stop()

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.firstEventShouldBeStart()
            recordedEvents.records.shouldContain(
                Record(EventAndArgument(SwitchEvent, null), PROCESSED),
            )
            recordedEvents.records shouldHaveSize 3 // StopEvent
        }

        "check recorded events with DestroyEvent" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                initialState()
                transition<SwitchEvent>()
            }
            machine.processEvent(SwitchEvent)
            machine.destroy()

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.firstEventShouldBeStart()
            recordedEvents.records.shouldContain(
                Record(EventAndArgument(SwitchEvent, null), PROCESSED),
            )
            recordedEvents.records shouldHaveSize 3 // DestroyEvent
        }

        "check recorded events with FinishedEvent" {
            lateinit var state2: State
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                state2 = state("state2")
                initialState("state1") {
                    initialFinalState()
                    transition<FinishedEvent>(targetState = state2)
                }
            }

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.firstEventShouldBeStart()
            recordedEvents.records shouldHaveSize 1
            machine.activeStates().shouldContainExactly(state2)
        }

        "check recorded events on restart without ${EventRecordingArguments::clearRecordsOnMachineRestart.name} flag" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments {
                    eventRecordingArguments = buildEventRecordingArguments {
                        clearRecordsOnMachineRestart = false
                    }
                }
            ) {
                initialState()
                transition<FirstEvent>()
                transition<SecondEvent>()
            }
            machine.processEvent(FirstEvent)
            machine.restart()
            machine.processEvent(SecondEvent)

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.firstEventShouldBeStart()
            recordedEvents.records.shouldContainInOrder(
                Record(EventAndArgument(FirstEvent, null), PROCESSED),
                Record(EventAndArgument(SecondEvent, null), PROCESSED),
            )
            recordedEvents.records shouldHaveSize 5
        }

        "check recorded events on restart with ${EventRecordingArguments::clearRecordsOnMachineRestart.name} flag (default)" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                initialState()
                transition<FirstEvent>()
                transition<SecondEvent>()
            }
            machine.processEvent(FirstEvent)
            machine.restart()
            machine.processEvent(SecondEvent)

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.firstEventShouldBeStart()
            recordedEvents.records.shouldContain(
                Record(EventAndArgument(SecondEvent, null), PROCESSED),
            )
            recordedEvents.records shouldHaveSize 2
        }

        "check recorded events with ${EventRecordingArguments::skipIgnoredEvents.name} flag (default)" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
            ) {
                initialState()
            }
            machine.processEvent(SwitchEvent)

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.firstEventShouldBeStart()
            recordedEvents.records shouldHaveSize 1
        }

        "check recorded events without ${EventRecordingArguments::skipIgnoredEvents.name} flag" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments {
                    eventRecordingArguments = buildEventRecordingArguments {
                        skipIgnoredEvents = false
                    }
                }
            ) {
                initialState()
            }
            machine.processEvent(SwitchEvent)

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.firstEventShouldBeStart()
            recordedEvents.records.shouldContain(
                Record(EventAndArgument(SwitchEvent, null), IGNORED),
            )
            recordedEvents.records shouldHaveSize 2
        }
    }
})

private fun RecordedEvents.firstEventShouldBeStart() {
    val firstEvent = records.first().eventAndArgument.event
    firstEvent.shouldBeInstanceOf<SerializableGeneratedEvent>()
    firstEvent.eventType shouldBe SerializableGeneratedEvent.EventType.Start
}