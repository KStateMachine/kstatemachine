/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2026.
 * All rights reserved.
 */

package ru.nsk.samples

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.State
import ru.nsk.kstatemachine.state.autoTransition
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.onEntry
import ru.nsk.kstatemachine.state.state
import ru.nsk.kstatemachine.state.transitionOn
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.transition.TransitionType
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Classic splash-screen pattern: the splash auto-advances to home after a short delay, home
 * auto-advances to a screensaver after a longer one. A user input ([UserInput]) re-enters home,
 * which restarts the screensaver timer.
 *
 * Shows [autoTransition] with the optional `delay` argument (requires `kstatemachine-coroutines`).
 */
private object UserInput : Event

fun main() = runBlocking {
    lateinit var splash: State
    lateinit var home: State
    lateinit var screensaver: State

    val homeEntered = CompletableDeferred<Unit>()
    val screensaverEntered = CompletableDeferred<Unit>()

    val machine = createStateMachine(this, name = "DelayedAutoTransitionSample") {
        logger = StateMachine.Logger { println(it()) }

        screensaver = state("screensaver") {
            onEntry { screensaverEntered.complete(Unit) }
        }
        home = state("home") {
            onEntry { homeEntered.complete(Unit) }
            autoTransition(delay = 80.milliseconds, targetState = screensaver)
            // Re-enter home on user input — the EXTERNAL self-targeted transition forces
            // exit + entry, restarting the screensaver timer from zero.
            transitionOn<UserInput>("refresh") {
                targetState = { home }
                type = TransitionType.EXTERNAL
            }
        }
        splash = initialState("splash") {
            autoTransition(delay = 40.milliseconds, targetState = home)
        }
    }

    withTimeout(5.seconds) { homeEntered.await() }
    check(home.isActive) { "splash should have advanced to home" }

    // Send user input — processEvent is synchronous, so home is still active right after.
    machine.processEvent(UserInput)
    check(home.isActive) { "user input re-enters home, resetting the screensaver timer" }

    withTimeout(5.seconds) { screensaverEntered.await() }
    check(screensaver.isActive) { "screensaver should fire after timer restart" }
    println("Done")
}
