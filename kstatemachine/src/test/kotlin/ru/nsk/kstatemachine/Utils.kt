package ru.nsk.kstatemachine


object SwitchEvent : UnitEvent()
object SwitchEventL1 : UnitEvent()
object SwitchEventL2 : UnitEvent()

object FirstEvent : UnitEvent()
object SecondEvent : UnitEvent()

interface Callbacks {
    fun onStarted(machine: StateMachine)
    fun onStopped(machine: StateMachine)
    fun onStateChanged(state: State)
    fun onFinished(state: State)
    fun onIgnoredEvent(event: Event)
    fun onTriggeredTransition(event: Event)
    fun onTriggeredTransition(event: Event, index: Int)
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