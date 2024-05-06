package ru.nsk.kstatemachine.persistence

import ru.nsk.kstatemachine.event.DestroyEvent
import ru.nsk.kstatemachine.event.StartEvent
import ru.nsk.kstatemachine.event.StopEvent
import ru.nsk.kstatemachine.statemachine.*
import ru.nsk.kstatemachine.statemachine.StateMachine.EventRecordingArguments
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
    val records: List<Record>,
)

data class Record(
    val eventAndArgument: EventAndArgument<*>,
    val processingResult: ProcessingResult,
)

internal class EventRecorderImpl(
    private val machine: StateMachine,
    private val arguments: EventRecordingArguments
) : EventRecorder {
    private val records = mutableListOf<Record>()

    /**
     * Should be called with not wrapped event.
     * Should not be called on [ProcessingResult.PENDING] events.
     */
    fun onProcessEvent(eventAndArgument: EventAndArgument<*>, processingResult: ProcessingResult) {
        val lastEvent = records.lastOrNull()?.eventAndArgument?.event
        check(lastEvent !is DestroyEvent) {
            "Internal error, ${::onProcessEvent::name} called after " +
                    "${DestroyEvent::class.simpleName} processing, which is considered as last possible event"
        }
        if (arguments.skipIgnoredEvents && processingResult == ProcessingResult.IGNORED) return
        if (arguments.clearRecordsOnMachineRestart && lastEvent is StopEvent) records.clear()
        records += Record(eventAndArgument, processingResult)
    }

    override fun getRecordedEvents(): RecordedEvents {
        return RecordedEvents(machine.structureHashCode, records)
    }
}

data class RestorationResult(
    val results: List<RestoredEventResult>
)

data class RestoredEventResult(
    val record: Record,
    val processingResult: Result<ProcessingResult>,
    val warnings: List<Exception>,
)

fun interface RestorationResultValidator {
    /**
     * Throws if validation is not passed
     */
    fun validate(result: RestorationResult)
}

/**
 * Completely skips validation
 */
object EmptyValidator : RestorationResultValidator {
    override fun validate(result: RestorationResult) = Unit
}

/**
 * Does not allow warnings or failed processing results
 */
object StrictValidator : RestorationResultValidator {
    override fun validate(result: RestorationResult) {
        result.results.forEach {
            if (it.warnings.isNotEmpty()) {
                throw RestorationResultValidationException(
                    "The ${RestorationResult::class.simpleName} contains warnings",
                    result,
                )
            }
            if (it.processingResult.isFailure) {
                throw RestorationResultValidationException(
                    "The ${RestorationResult::class.simpleName} contains failed processing result",
                    result,
                )
            }
        }
    }
}

class RestorationResultValidationException(
    message: String,
    val result: RestorationResult
) : RuntimeException(message)

/**
 * Processes [RecordedEvents] with purpose of restoring a [StateMachine] to a state configuration as it was before.
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
    validator: RestorationResultValidator = StrictValidator,
): Unit = coroutineAbstraction.withContext {
    if (isRunning) {
        restoreRunningMachineByRecordedEvents(recordedEvents, muteListeners, disableStructureHashCodeCheck, validator)
    } else {
        onStarted {
            restoreRunningMachineByRecordedEvents(
                recordedEvents,
                muteListeners,
                disableStructureHashCodeCheck,
                validator,
            )
        }
    }
}

/**
 * May be called on started ([StateMachine.isRunning] == true) [StateMachine] only,
 * and returns [RestorationResult] allowing to inspect how the restoration was processed.
 *
 * There is no way on library side to decide if some exceptions during event processing are errors or not.
 * For instance [StateMachine] may be configured with [throwingIgnoredEventHandler] so some exceptions might
 * be expected and are not really errors.
 */
suspend fun StateMachine.restoreRunningMachineByRecordedEvents(
    recordedEvents: RecordedEvents,
    muteListeners: Boolean = true,
    disableStructureHashCodeCheck: Boolean = false,
    validator: RestorationResultValidator = StrictValidator,
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
    val results = mutableListOf<RestoredEventResult>()
    val mutationSection = if (muteListeners) openListenersMutationSection() else EmptyListenersMutationSection
    mutationSection.use {
        for (record in recordedEvents.records) {
            val warnings = mutableListOf<Exception>()
            val (event, argument) = record.eventAndArgument
            if (event is StartEvent)
                continue // fixme вызов start мог иметь argument что с ним делать?
            val processingResult = runCatching { processEvent(event, argument) }
            val actualResult = processingResult.getOrNull() // fixme может вернуться panding, надо пропускать?
            if (actualResult != null && actualResult != record.processingResult)
                warnings += IllegalStateException("Recorded and actual processing results does not match")
            results += RestoredEventResult(record, processingResult, warnings)
        }
    }
    RestorationResult(results).also {
        validator.validate(it)
    }
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