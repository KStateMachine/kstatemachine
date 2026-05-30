/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.samples

import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine

private object HistoryStateSample {
    object SwitchEvent : Event
    object PauseEvent : Event
    object ResumeEvent : Event
}

/**
 * [HistoryState] remembers the last active sub-state of its parent.
 * When transitioning to a [HistoryState], the machine restores the previously active sub-state
 * instead of always starting from the initial one.
 * On the very first entry (no stored state yet), [HistoryState] falls back to [defaultState].
 */
fun main() = runBlocking {
    val state11: State
    lateinit var state12: State
    lateinit var state2: State
    lateinit var history: HistoryState

    val machine = createStateMachine(this) {
        logger = StateMachine.Logger { println(it()) }

        state2 = state("state2") {
            transitionOn<HistoryStateSample.ResumeEvent> { targetState = { history } }
        }

        initialState("state1") {
            state11 = initialState("state11") {
                transitionOn<HistoryStateSample.SwitchEvent> { targetState = { state12 } }
            }
            state12 = state("state12") {
                transitionOn<HistoryStateSample.PauseEvent> { targetState = { state2 } }
            }
            history = historyState("history", defaultState = state11)
        }
    }

    // initial: state11 is active (history defaultState)
    check(state11 in machine.activeStates())

    // navigate to state12 within state1
    machine.processEvent(HistoryStateSample.SwitchEvent)
    check(state12 in machine.activeStates())

    // pause: leave state1 entirely
    machine.processEvent(HistoryStateSample.PauseEvent)
    check(state12 !in machine.activeStates())

    // resume via history: restores state12 (not state11)
    machine.processEvent(HistoryStateSample.ResumeEvent)
    check(state12 in machine.activeStates())
    println("History restored: ${machine.activeStates().map { it.name }}")
}
