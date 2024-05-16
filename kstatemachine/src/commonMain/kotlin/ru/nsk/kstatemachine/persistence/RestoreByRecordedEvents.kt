package ru.nsk.kstatemachine.persistence

import ru.nsk.kstatemachine.event.StartEvent
import ru.nsk.kstatemachine.statemachine.*
import ru.nsk.kstatemachine.visitors.structureHashCode

data class RestorationResult(
    val results: List<RestoredEventResult>,
    val warnings: List<RestorationWarningException>,
)

data class RestoredEventResult(
    val record: Record,
    val processingResult: Result<ProcessingResult>,
    val warnings: List<RestorationWarningException>,
)

enum class WarningType {
    ProcessingResultNotMatch,
    RecordedAndProcessedEventCountNotMatch,
}

class RestorationWarningException(
    val warningType: WarningType,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Processes [RecordedEvents] with purpose of restoring a [StateMachine] to a state configuration as it was before
 * (when the record was made).
 * Starts the [StateMachine] if necessary and returns [RestorationResult] allowing to inspect
 * how the restoration was processed. Specified [RestorationResultValidator] will be called to validate the result so
 * do not have to remember to perform validation of the [RestorationResult].
 *
 * There is no way on library side to strictly decide if some exceptions during event processing are errors or not.
 * For instance [StateMachine] may be configured with [throwingIgnoredEventHandler] so some exceptions might
 * be expected and are not really errors.
 * So this method collects all such warnings into [RestorationResult] object in a form of
 * [RestorationWarningException] collections, delegating the responsibility for them to
 * [RestorationResultValidator] object. [StrictValidator] is used by default and the library provides
 * standard [EmptyValidator] to explicitly skip the validation step if necessary.
 *
 * @param muteListeners listeners are not triggered by default,
 * as we assume that client code reactions were already processed before.
 * @param disableStructureHashCodeCheck allows to skip the machine structure check
 * to force processing of [RecordedEvents]. Note that running the same event sequence on similar machines but having
 * different structureHashCode value, may produce different results more likely.
 */
suspend fun StateMachine.restoreByRecordedEvents(
    recordedEvents: RecordedEvents,
    muteListeners: Boolean = true,
    disableStructureHashCodeCheck: Boolean = false,
    validator: RestorationResultValidator = StrictValidator,
): RestorationResult = coroutineAbstraction.withContext {
    checkNotDestroyed()
    if (isRunning) {
        check(!hasProcessedEvents) {
            "$this has already processed events, ${::restoreByRecordedEvents.name}() operation only makes " +
                    "sense on initially clear ${StateMachine::class.simpleName}, please call it before " +
                    "processing any other events (or even before start - optionally)"
        }
    }

    if (!disableStructureHashCodeCheck)
        check(structureHashCode == recordedEvents.structureHashCode) {
            "$this structure seems to be different from recorded original one, you can disable this error by the " +
                    "disableStructureHashCodeCheck argument if you are sure that it is correct"
        }

    this as InternalStateMachine
    val results = mutableListOf<RestoredEventResult>()
    val commonWarnings = mutableListOf<RestorationWarningException>()
    val mutationSection = if (muteListeners) openListenersMutationSection() else EmptyListenersMutationSection
    mutationSection.use {
        recordedEvents.records.forEachIndexed iteration@{ index, record ->
            val warnings = mutableListOf<RestorationWarningException>()
            val (event, argument) = record.eventAndArgument
            if (event is StartEvent) {
                if (isRunning) {
                    if (argument == null) {
                        results += RestoredEventResult(record, Result.success(ProcessingResult.PROCESSED), warnings)
                        return@iteration // continue
                    } else {
                        if (index == 0)
                            error(
                                "The ${StateMachine::class.simpleName} is already started, but " +
                                        "the ${RecordedEvents::class.simpleName} contains an argument for " +
                                        "${StateMachine::start.name} method. " +
                                        "To restore such machine, " +
                                        "do not start it before calling ${::restoreByRecordedEvents.name}"
                            )
                        else {
                            destroy()
                            error("The machine should not be running here. Internal error. Never get here")
                        }
                    }
                } else {
                    start(argument)
                    results += RestoredEventResult(record, Result.success(ProcessingResult.PROCESSED), warnings)
                }
            } else {
                val processingResult = runCatching { processEvent(event, argument) }
                val actualResult = processingResult.getOrNull()
                if (actualResult != null && actualResult != record.processingResult) {
                    warnings += RestorationWarningException(
                        WarningType.ProcessingResultNotMatch,
                        "Recorded (${record.processingResult}) and actual ($actualResult) processing results does not match",
                    )
                }
                results += RestoredEventResult(record, processingResult, warnings)
            }
        }
    }
    if (results.size != recordedEvents.records.size)
        commonWarnings += RestorationWarningException(
            WarningType.RecordedAndProcessedEventCountNotMatch,
            "Recorded event count is ${recordedEvents.records.size} but the actual processed event count is " +
                    "${results.size}. They should not differ, this should never happen",
        )
    RestorationResult(results, commonWarnings).also {
        validator.validate(it, recordedEvents, this)
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