package ru.nsk.kstatemachine


object SwitchEvent : Event
object SwitchEventL1 : Event
object SwitchEventL2 : Event

object FirstEvent : Event
object SecondEvent : Event

interface Callbacks {
    fun onStarted()
    fun onStateChanged(state: State)
    fun onFinished(state: State)
    fun onIgnoredEvent(event: Event)
    fun onTriggeredTransition(event: Event)
    fun onEntryState(state: State)
    fun onExitState(state: State)
}