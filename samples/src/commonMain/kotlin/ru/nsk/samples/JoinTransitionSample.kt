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
import ru.nsk.samples.JoinTransitionSample.DownloadCompleteEvent
import ru.nsk.samples.JoinTransitionSample.ValidationCompleteEvent

private object JoinTransitionSample {
    object DownloadCompleteEvent : Event
    object ValidationCompleteEvent : Event
}

/**
 * Demonstrates a UML join pseudo-state using [joinTransition].
 *
 * Two parallel regions (download & validate) each fire an independent event.
 * Only when BOTH have reached their join-point states does the machine
 * transition to the "processing" state.
 *
 * This is the complement of [targetParallelStates] (fork).
 */
fun main() = runBlocking {
    // Declared outside the builder so they can be referenced in the checks below
    lateinit var parallelWork: State
    lateinit var processing: State
    lateinit var downloadJoin: IState
    lateinit var validationJoin: IState

    val machine = createStateMachine(this, name = "JoinTransitionSample") {
        logger = StateMachine.Logger { println(it()) }

        // processing is declared first so joinTransition below can reference it directly
        processing = state("processing") {
            onEntry { println("Both tasks done → processing") }
        }

        // parallelWork is the initial state; region lambdas run sequentially before joinTransition,
        // so downloadJoin / validationJoin are assigned by the time joinTransition is called.
        parallelWork = initialState("parallelWork", childMode = ChildMode.PARALLEL) {
            state("download") {
                downloadJoin = state("downloadJoin")   // no outgoing transitions → soft blocking
                initialState("downloading") {
                    transition<DownloadCompleteEvent> { targetState = downloadJoin }
                }
            }
            state("validate") {
                validationJoin = state("validationJoin")
                initialState("validating") {
                    transition<ValidationCompleteEvent> { targetState = validationJoin }
                }
            }
            // Fires automatically once both join-point states are simultaneously active
            joinTransition(downloadJoin, validationJoin, name = "join", targetState = processing)
        }
    }

    machine.processEvent(DownloadCompleteEvent)
    // Only download region done — still in parallelWork
    check(parallelWork in machine.activeStates())

    machine.processEvent(ValidationCompleteEvent)
    // Both regions joined — machine transitions to processing
    check(processing in machine.activeStates())
    println("Done")
}
