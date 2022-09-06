package ru.nsk.kstatemachine

/**
 * Returns [StateMachine.PendingEventHandler] implementation that throws exception. This is an old default behaviour.
 */
fun StateMachine.throwingPendingEventHandler() = StateMachine.PendingEventHandler { pendingEvent, _ ->
    error(
        "$this can not process pending $pendingEvent as event processing is already running. " +
                "Do not call processEvent() from notification listeners or use queuePendingEventHandler()"
    )
}

fun StateMachine.queuePendingEventHandler(): StateMachine.PendingEventHandler = QueuePendingEventHandler(this)

internal class QueuePendingEventHandler(private val machine: StateMachine) : StateMachine.PendingEventHandler {
    private val queue = ArrayDeque<EventAndArgument<*>>()

    fun checkEmpty() = check(queue.isEmpty()) { "Event queue is not empty, internal error" }

    override fun onPendingEvent(pendingEvent: Event, argument: Any?) {
        machine.log { "$machine queued event $pendingEvent with argument $argument " }
        queue.add(EventAndArgument(pendingEvent, argument))
    }

    fun nextEventAndArgument() = queue.removeFirstOrNull()

    fun clear() = queue.clear()
}