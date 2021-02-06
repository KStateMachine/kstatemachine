package ru.nsk.kstatemachine


object SwitchEvent : Event
object SwitchEventL1 : Event
object SwitchEventL2 : Event

object FirstEvent : Event
object SecondEvent : Event

interface Callbacks {
    fun onStarted(machine: StateMachine)
    fun onStateChanged(state: State)
    fun onFinished(state: State)
    fun onIgnoredEvent(event: Event)
    fun onTriggeredTransition(event: Event)
    fun onEntryState(state: State)
    fun onExitState(state: State)
}

fun Callbacks.listen(state: State) {
    state.onEntry { onEntryState(this) }
    state.onExit { onExitState(this) }
    state.onFinished { onFinished(this) }
}

inline fun <reified E : Event> Callbacks.listen(transitionBuilder: TransitionBuilder<E>) {
    transitionBuilder.onTriggered { onTriggeredTransition(it.event) }
}