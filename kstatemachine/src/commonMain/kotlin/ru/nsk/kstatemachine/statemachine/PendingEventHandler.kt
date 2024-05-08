package ru.nsk.kstatemachine.statemachine

import ru.nsk.kstatemachine.state.log
import ru.nsk.kstatemachine.transition.EventAndArgument

/**
 * Returns [StateMachine.PendingEventHandler] implementation that throws exception. This is an old default behaviour.
 */
fun StateMachine.throwingPendingEventHandler() = StateMachine.PendingEventHandler {
    error(
        "$this can not process pending ${it.event} as event processing is already running. " +
                "Do not call processEvent() from notification listeners or use queuePendingEventHandler()"
    )
}

fun StateMachine.queuePendingEventHandler(): QueuePendingEventHandler = QueuePendingEventHandlerImpl(this)

interface QueuePendingEventHandler : StateMachine.PendingEventHandler {
    suspend fun checkEmpty()
    suspend fun nextEventAndArgument(): EventAndArgument<*>?
    suspend fun clear()
}

private class QueuePendingEventHandlerImpl(private val machine: StateMachine) : QueuePendingEventHandler {
    private val queue = ArrayDeque<EventAndArgument<*>>()

    override suspend fun checkEmpty() = check(queue.isEmpty()) {
        "Event queue is not empty, internal error (should never happen). " +
                "Double check that you don't break multi-threading usage rules, if you see this error. " +
                "Usually it is related to concurrent collection modification."
    }

    override suspend fun onPendingEvent(eventAndArgument: EventAndArgument<*>) {
        machine.log {
            "$machine queued event ${eventAndArgument.event::class.simpleName} with argument ${eventAndArgument.argument}"
        }
        queue.add(eventAndArgument)
    }

    override suspend fun nextEventAndArgument() = queue.removeFirstOrNull()

    override suspend fun clear() = queue.clear()
}