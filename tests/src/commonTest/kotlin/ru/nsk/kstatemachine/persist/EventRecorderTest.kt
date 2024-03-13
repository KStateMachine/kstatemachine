package ru.nsk.kstatemachine.persist

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import ru.nsk.kstatemachine.FirstEvent
import ru.nsk.kstatemachine.SecondEvent
import ru.nsk.kstatemachine.SwitchEvent
import ru.nsk.kstatemachine.event.UndoEvent
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.StateMachine.CreationArguments
import ru.nsk.kstatemachine.statemachine.createStdLibStateMachine
import ru.nsk.kstatemachine.statemachine.destroy
import ru.nsk.kstatemachine.statemachine.undo
import ru.nsk.kstatemachine.transition.EventAndArgument
import ru.nsk.kstatemachine.visitors.structureHashCode

class EventRecorderTest : StringSpec({
    "negative event recording should be explicitly enabled" {
        val machine = createStdLibStateMachine {
            initialState()
        }
        shouldThrowWithMessage<IllegalStateException>(
            "Event recording is not enabled. Use ${CreationArguments::recordEvents.name} parameter" +
                    " of createStateMachine() method family"
        ) { machine.eventRecorder }
    }

    "negative check ${StateMachine::restoreRunningMachineByRecordedEvents.name} on not running machine throws" {
        val recordedEvents = RecordedEvents(0, emptyList())

        val machine = createStdLibStateMachine(start = false) {
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

        val machine = createStdLibStateMachine {
            initialState()
        }
        machine.destroy(stop = false)

        shouldThrowWithMessage<IllegalStateException>("$machine is already destroyed") {
            machine.restoreRunningMachineByRecordedEvents(recordedEvents)
        }
    }

    "negative check ${StateMachine::restoreRunningMachineByRecordedEvents.name} on machine that already processed events throws" {
        val recordedEvents = RecordedEvents(0, emptyList())

        val machine = createStdLibStateMachine {
            initialState()
        }
        machine.processEvent(SwitchEvent)

        shouldThrowWithMessage<IllegalStateException>(
            "$machine has already processed events, ${StateMachine::restoreRunningMachineByRecordedEvents.name}() " +
                    "operation only makes sense on initially clear ${StateMachine::class.simpleName}, please call it before " +
                    "processing any other events"
        ) { machine.restoreRunningMachineByRecordedEvents(recordedEvents) }
    }

    "check event recording preconditions without structure check" {
        val machine1 = createStdLibStateMachine(creationArguments = CreationArguments(recordEvents = true)) {
            initialState()
        }
        val recordedEvents = machine1.eventRecorder.getRecordedEvents()

        val machine2 = createStdLibStateMachine {
            initialState()
        }
        shouldNotThrowAny {
            machine2.restoreByRecordedEventsBlocking(recordedEvents, disableStructureHashCodeCheck = true)
        }
    }

    "check event recording preconditions with structure check" {
        val machine1 = createStdLibStateMachine(creationArguments = CreationArguments(recordEvents = true)) {
            initialState()
        }
        val recordedEvents = machine1.eventRecorder.getRecordedEvents()

        val machine2 = createStdLibStateMachine(creationArguments = CreationArguments(recordEvents = true)) {
            initialState()
        }
        shouldNotThrowAny { machine2.restoreByRecordedEventsBlocking(recordedEvents) }
    }

    "check recorded events with arguments and undo" {
        val machine = createStdLibStateMachine(
            creationArguments = CreationArguments(recordEvents = true, isUndoEnabled = true)
        ) {
            initialState()
        }
        machine.processEvent(FirstEvent)
        machine.processEvent(SecondEvent, 2)
        machine.undo()

        val recordedEvents = machine.eventRecorder.getRecordedEvents()
        recordedEvents.structureHashCode shouldBe machine.structureHashCode
        recordedEvents.events.shouldContainExactly(
            EventAndArgument(FirstEvent, null),
            EventAndArgument(SecondEvent, 2),
            EventAndArgument(UndoEvent, null),
        )
    }
//
//    "check recorded events with StopEvent" {
//        TODO()
//    }
//
//    "check recorded events with DestroyEvent" {
//        TODO()
//    }
})
