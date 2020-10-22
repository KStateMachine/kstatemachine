package ru.nsk.kstatemachine

interface Callbacks {
    fun onTriggeringEvent(event: Event)
    fun onEntryState(state: State)
    fun onExitState(state: State)
}