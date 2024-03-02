package ru.nsk.kstatemachine.persist

import ru.nsk.kstatemachine.statemachine.*
import ru.nsk.kstatemachine.transition.EventAndArgument
import ru.nsk.kstatemachine.visitors.structureHashCode

/**
 * Watches all incoming events with purpose of repeating them later on clean (just constructed) [StateMachine] instance
 * to reproduce exactly the same [StateMachine] state as before
 */
interface EventRecorder {
    fun getRecordedEvents(): RecordedEvents
}

/**
 * This object is intended to be serialized by client code
 */
data class RecordedEvents(
    val structureHashCode: Int,
    val events: List<EventAndArgument<*>>,
)

internal class EventRecorderImpl(
    private val machine: StateMachine
) : EventRecorder {
    private val events = mutableListOf<EventAndArgument<*>>()

    fun onProcessEvent(eventAndArgument: EventAndArgument<*>) {
        events += eventAndArgument
    }

    override fun getRecordedEvents(): RecordedEvents {
        return RecordedEvents(machine.structureHashCode, events)
    }
}

data class RestorationResult(
    val results: List<RestoredEventResult>
)

data class RestoredEventResult(
    val event: EventAndArgument<*>,
    val processingResult: Result<ProcessingResult>,
)

/**
 * Processes [RecordedEvents] with purpose of restoring [StateMachine] state as it was before.
 *
 * @param muteListeners listeners are not triggered by default,
 * as I assume that client code reactions were already processed before.
 * @param disableStructureHashCodeCheck allows to skip the machine structure check
 * to force processing of [RecordedEvents]. Note that running the same event sequence on similar machines but having
 * different structureHashCode value, may produce different results more likely.
 */
suspend fun StateMachine.restoreByRecordedEvents(
    recordedEvents: RecordedEvents,
    muteListeners: Boolean = true,
    disableStructureHashCodeCheck: Boolean = false,
): Unit = coroutineAbstraction.withContext {
    if (isRunning) {
        restoreRunningMachineByRecordedEvents(recordedEvents, muteListeners, disableStructureHashCodeCheck)
    } else {
        onStarted {
            restoreRunningMachineByRecordedEvents(recordedEvents, muteListeners, disableStructureHashCodeCheck)
        }
    }
}

/**
 * May be called on started ([StateMachine.isRunning] == true) [StateMachine] only,
 * and returns [RestorationResult] allowing to inspect how the restoration was processed.
 */
suspend fun StateMachine.restoreRunningMachineByRecordedEvents(
    recordedEvents: RecordedEvents,
    muteListeners: Boolean = true,
    disableStructureHashCodeCheck: Boolean = false,
): RestorationResult = coroutineAbstraction.withContext {
    check(isRunning) {
        "$this is not running, ${::restoreRunningMachineByRecordedEvents.name}() operation only makes sense on " +
                "created and started ${StateMachine::class.simpleName}, please call it after the machine is started"
    }
    checkNotDestroyed()
    check(!hasProcessedEvents) {
        "$this has already processed events, ${::restoreRunningMachineByRecordedEvents.name}() operation only makes " +
                "sense on initially clear ${StateMachine::class.simpleName}, please call it before " +
                "processing any other events"
    }

    if (!disableStructureHashCodeCheck)
        check(structureHashCode == recordedEvents.structureHashCode) {
            "$this structure seems to be different from recorded original one"
        }

    this as InternalStateMachine
    val mutationSection = if (muteListeners) openListenersMutationSection() else EmptyListenersMutationSection

    val results = mutableListOf<RestoredEventResult>()
    mutationSection.use {
        recordedEvents.events.forEach {
            val processingResult = runCatching { processEvent(it.event, it.argument) }
            results += RestoredEventResult(it, processingResult)
        }
    }
    RestorationResult(results)
}

/**
 * Blocking [restoreRunningMachineByRecordedEvents] alternative
 */
fun StateMachine.restoreRunningMachineByRecordedEventsBlocking(
    recordedEvents: RecordedEvents,
    muteListeners: Boolean = true,
    disableStructureHashCodeCheck: Boolean = false,
): RestorationResult {
    return coroutineAbstraction.runBlocking {
        restoreRunningMachineByRecordedEvents(recordedEvents, muteListeners, disableStructureHashCodeCheck)
    }
}

/**
 * Blocking [restoreByRecordedEvents] alternative
 */
fun StateMachine.restoreByRecordedEventsBlocking(
    recordedEvents: RecordedEvents,
    muteListeners: Boolean = true,
    disableStructureHashCodeCheck: Boolean = false,
) {
    coroutineAbstraction.runBlocking {
        restoreByRecordedEvents(recordedEvents, muteListeners, disableStructureHashCodeCheck)
    }
}

private object EmptyListenersMutationSection : ListenersMutationSection {
    override fun close() = Unit
}