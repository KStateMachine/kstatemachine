package ru.nsk.kstatemachine.persist

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
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
import ru.nsk.kstatemachine.statemachine.StateMachine.CreationArguments
import ru.nsk.kstatemachine.transition.EventAndArgument
import ru.nsk.kstatemachine.visitors.structureHashCode

class EventRecorderTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "negative event recording should be explicitly enabled" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState()
            }
            shouldThrowWithMessage<IllegalStateException>(
                "Event recording is not enabled. Use ${CreationArguments::recordEvents.name} parameter" +
                        " of createStateMachine() method family"
            ) { machine.eventRecorder }
        }

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
            shouldThrowWithMessage<IllegalStateException>(
                "$machine is not running, ${StateMachine::restoreRunningMachineByRecordedEvents.name}() " +
                        "operation only makes sense on created and started ${StateMachine::class.simpleName}, " +
                        "please call it after the machine is started"
            ) { machine.restoreRunningMachineByRecordedEvents(recordedEvents) }
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

        "check event recording preconditions without structure check" {
            val machine1 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(recordEvents = true)
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

        "check event recording preconditions with structure check" {
            val machine1 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(recordEvents = true)
            ) {
                initialState()
            }
            val recordedEvents = machine1.eventRecorder.getRecordedEvents()

            val machine2 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(recordEvents = true)
            ) {
                initialState()
            }
            shouldNotThrowAny { machine2.restoreByRecordedEventsBlocking(recordedEvents) }
        }

        "check recorded events with arguments" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(recordEvents = true)
            ) {
                initialState()
            }
            machine.processEvent(FirstEvent)
            machine.processEvent(SecondEvent, 2)

            val recordedEvents = machine.eventRecorder.getRecordedEvents()
            recordedEvents.structureHashCode shouldBe machine.structureHashCode

            recordedEvents.events.first().event.shouldBeInstanceOf<StartEvent>()
            recordedEvents.events.shouldContainInOrder(
                EventAndArgument(FirstEvent, null),
                EventAndArgument(SecondEvent, 2),
            )
        }

        "check recorded events and undo" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(recordEvents = true, isUndoEnabled = true)
            ) {
                initialState()
            }
            machine.processEvent(SwitchEvent)
            machine.undo()

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.events.first().event.shouldBeInstanceOf<StartEvent>()
            recordedEvents.events.shouldContainInOrder(
                EventAndArgument(SwitchEvent, null),
                EventAndArgument(UndoEvent, null),
            )
        }

        "check recorded events with StopEvent" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(recordEvents = true, isUndoEnabled = true)
            ) {
                initialState()
            }
            machine.processEvent(SwitchEvent)
            machine.stop()

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.events.first().event.shouldBeInstanceOf<StartEvent>()
            recordedEvents.events.shouldContain(EventAndArgument(SwitchEvent, null))
            recordedEvents.events shouldHaveSize 3 // StopEvent
        }

        "check recorded events with DestroyEvent" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(recordEvents = true, isUndoEnabled = true)
            ) {
                initialState()
            }
            machine.processEvent(SwitchEvent)
            machine.destroy()

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.events.first().event.shouldBeInstanceOf<StartEvent>()
            recordedEvents.events.shouldContain(EventAndArgument(SwitchEvent, null))
            recordedEvents.events shouldHaveSize 3 // DestroyEvent
        }

        "check recorded events on restart" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = CreationArguments(recordEvents = true, isUndoEnabled = true)
            ) {
                initialState()
            }
            machine.processEvent(FirstEvent)
            machine.restart()
            machine.processEvent(SecondEvent)

            val recordedEvents = machine.eventRecorder.getRecordedEvents()

            recordedEvents.events.first().event.shouldBeInstanceOf<StartEvent>()
            recordedEvents.events.shouldContainInOrder(
                EventAndArgument(FirstEvent, null), // fixme should be cleared?
                EventAndArgument(SecondEvent, null),
            )
            recordedEvents.events shouldHaveSize 5 // fixme should be cleared?
        }
    }
})
