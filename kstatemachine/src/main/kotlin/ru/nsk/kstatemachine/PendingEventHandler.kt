package ru.nsk.kstatemachine

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
    fun checkEmpty()
    fun nextEventAndArgument(): EventAndArgument<*>?
    fun clear()
}

private class QueuePendingEventHandlerImpl(private val machine: StateMachine) : QueuePendingEventHandler {
    private val queue = ArrayDeque<EventAndArgument<*>>()

    override fun checkEmpty() = check(queue.isEmpty()) { "Event queue is not empty, internal error" }

    override fun onPendingEvent(eventAndArgument: EventAndArgument<*>) {
        machine.log {
            "$machine queued event ${eventAndArgument.event::class.simpleName} with argument ${eventAndArgument.argument}"
        }
        queue.add(eventAndArgument)
    }

    override fun nextEventAndArgument() = queue.removeFirstOrNull()

    override fun clear() = queue.clear()
}