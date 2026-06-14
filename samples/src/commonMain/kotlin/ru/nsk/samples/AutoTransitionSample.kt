/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.samples

import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.state.State
import ru.nsk.kstatemachine.state.autoTransition
import ru.nsk.kstatemachine.state.autoTransitionOn
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.state
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.statemachine.restart

/**
 * Demonstrates UML eventless ("always") transitions using [autoTransition].
 *
 * The machine walks A → B → C purely from entry hooks — no external events. The last sample also
 * shows a guarded eventless transition that holds in place until the guard returns true on the
 * next entry (here triggered by a restart).
 */
fun main() = runBlocking {
    lateinit var b: State
    lateinit var c: State
    lateinit var guarded: State
    var ready = false

    val machine = createStateMachine(this, name = "AutoTransitionSample") {
        logger = StateMachine.Logger { println(it()) }

        guarded = state("guarded")
        c = state("c") {
            // Guarded eventless transition — stays put on first entry (ready=false).
            // On restart with ready=true the transition fires.
            autoTransitionOn(name = "c->guarded") {
                guard = { ready }
                targetState = { guarded }
            }
        }
        b = state("b") {
            autoTransition("b->c", targetState = c)
        }
        initialState("a") {
            autoTransition("a->b", targetState = b)
        }
    }

    // Entering "a" triggered "a->b", then "b->c". The guard on "c->guarded" rejected, so we stop in "c".
    check(c.isActive)
    check(!guarded.isActive)

    ready = true
    machine.restart()

    // Same chain, but this time the guard on "c->guarded" accepts.
    check(guarded.isActive)
    println("Done")
}
