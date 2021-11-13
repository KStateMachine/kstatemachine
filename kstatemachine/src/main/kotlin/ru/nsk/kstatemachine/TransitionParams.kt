package ru.nsk.kstatemachine

/**
 * Base interface for events which may trigger transitions of [StateMachine]
 */
interface Event

/**
 * Event holding some data
 */
interface DataEvent<out D> : Event {
    val data: D
}

@StateMachineDslMarker
data class TransitionParams<E : Event>(
    val transition: Transition<E>,
    val direction: TransitionDirection,
    val event: E,
    /**
     * This parameter may be used to pass arbitrary data with the event,
     * so there is no need to define [Event] subclasses every time.
     * Subclassing should be preferred if the event always contains data of some type.
     */
    val argument: Any? = null,
)