package ru.nsk.kstatemachine


object SwitchEvent : Event
object FirstEvent : Event
object SecondEvent : Event

interface Callbacks {
    fun onIgnoredEvent(event: Event)
    fun onTriggeringEvent(event: Event)
    fun onEntryState(state: State)
    fun onExitState(state: State)
}