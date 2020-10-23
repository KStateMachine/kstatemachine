package ru.nsk.kstatemachine


object SwitchEvent : Event

interface Callbacks {
    fun onTriggeringEvent(event: Event)
    fun onEntryState(state: State)
    fun onExitState(state: State)
}