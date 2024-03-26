package ru.nsk.kstatemachine.statemachine

/**
 * Returns [StateMachine.IgnoredEventHandler] implementation that throws exception.
 * This might be useful if you want to control that all events are handled (not skipped) by your [StateMachine].
 */
fun StateMachine.throwingIgnoredEventHandler(): StateMachine.IgnoredEventHandler {
    return StateMachine.IgnoredEventHandler {
        error(
            "${this@throwingIgnoredEventHandler} received ${it.event} that is going to be ignored. " +
                    "The machine was configured with ${StateMachine::throwingIgnoredEventHandler::name}, " +
                    "that forbids such behaviour."
        )
    }
}