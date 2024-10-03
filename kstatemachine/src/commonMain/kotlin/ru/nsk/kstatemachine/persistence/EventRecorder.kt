/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.persistence

import ru.nsk.kstatemachine.VisibleForTesting
import ru.nsk.kstatemachine.event.*
import ru.nsk.kstatemachine.event.DestroyEvent
import ru.nsk.kstatemachine.event.SerializableGeneratedEvent.*
import ru.nsk.kstatemachine.event.StartDataEventImpl
import ru.nsk.kstatemachine.event.StartEventImpl
import ru.nsk.kstatemachine.event.StopEvent
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
 * This class is intended to be serialized by client code
 */
class RecordedEvents @VisibleForTesting constructor(
    val structureHashCode: Int,
    val records: List<Record>,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as RecordedEvents

        if (structureHashCode != other.structureHashCode) return false
        if (records != other.records) return false

        return true
    }

    override fun hashCode(): Int {
        var result = structureHashCode
        result = 31 * result + records.hashCode()
        return result
    }

    override fun toString() = "RecordedEvents(structureHashCode=$structureHashCode, records=$records)"
}

class Record @VisibleForTesting constructor(
    val eventAndArgument: EventAndArgument<*>,
    val processingResult: ProcessingResult,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Record

        if (eventAndArgument != other.eventAndArgument) return false
        if (processingResult != other.processingResult) return false

        return true
    }

    override fun hashCode(): Int {
        var result = eventAndArgument.hashCode()
        result = 31 * result + processingResult.hashCode()
        return result
    }

    override fun toString() = "Record(eventAndArgument=$eventAndArgument, processingResult=$processingResult)"
}

internal class EventRecorderImpl(
    private val machine: StateMachine,
    private val arguments: EventRecordingArguments
) : EventRecorder {
    private val records = mutableListOf<Record>()

    /**
     * Should be called with not wrapped event in [eventAndArgument].
     * Should not be called on [ProcessingResult.PENDING] events.
     */
    fun onProcessEvent(eventAndArgument: EventAndArgument<*>, processingResult: ProcessingResult) {
        val lastEvent = records.lastOrNull()?.eventAndArgument?.event
        val lastEventType = (lastEvent as? SerializableGeneratedEvent)?.eventType

        check(lastEventType !is EventType.Destroy) {
            "Internal error, ${::onProcessEvent.name} called after " +
                    "${DestroyEvent::class.simpleName} processing, which is considered as last possible event"
        }
        if (arguments.skipIgnoredEvents && processingResult == ProcessingResult.IGNORED) return
        if (arguments.clearRecordsOnMachineRestart && lastEventType == EventType.Stop) records.clear()
        records += Record(eventAndArgument.transformGeneratedEvent(), processingResult)
    }

    override fun getRecordedEvents(): RecordedEvents {
        return RecordedEvents(machine.structureHashCode, records.toList() /* defensive copy */)
    }
}

private fun EventAndArgument<*>.transformGeneratedEvent(): EventAndArgument<*> {
    return if (event is GeneratedEvent) {
        val event = when (event) {
            is StartEvent -> SerializableGeneratedEvent(EventType.Start)
            is StopEvent -> SerializableGeneratedEvent(EventType.Stop)
            is DestroyEvent -> SerializableGeneratedEvent(EventType.Destroy(event.stop))
            is FinishedEvent -> TODO() // fixme ????
            is WrappedEvent -> error("Never get here")
            is SerializableGeneratedEvent -> error("Never get here, SerializableGeneratedEvent should not be processed") // fixme what if user sends this event?
        }
        EventAndArgument(event, argument)
    } else {
        this
    }
}