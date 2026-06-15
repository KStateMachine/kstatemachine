/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.samples

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.State
import ru.nsk.kstatemachine.state.delayedAutoTransition
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.state
import ru.nsk.kstatemachine.state.transitionOn
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.transition.TransitionType
import kotlin.time.Duration.Companion.milliseconds

/**
 * Classic splash-screen pattern: the splash auto-advances to home after a short delay; home
 * auto-advances to a screensaver after a longer one. A user input ([UserInput]) re-enters home,
 * which restarts the screensaver timer.
 */
private object UserInput : Event

fun main() = runBlocking {
    lateinit var splash: State
    lateinit var home: State
    lateinit var screensaver: State

    val machine = createStateMachine(this, name = "DelayedAutoTransitionSample") {
        logger = StateMachine.Logger { println(it()) }

        screensaver = state("screensaver")
        home = state("home") {
            delayedAutoTransition(delay = 80.milliseconds, targetState = screensaver)
            // Re-enter home on user input — the EXTERNAL self-targeted transition forces
            // exit + entry, restarting the screensaver timer from zero.
            transitionOn<UserInput>("refresh") {
                targetState = { home }
                type = TransitionType.EXTERNAL
            }
        }
        splash = initialState("splash") {
            delayedAutoTransition(delay = 40.milliseconds, targetState = home)
        }
    }

    delay(60.milliseconds)
    check(home.isActive) { "splash should have advanced to home" }

    delay(50.milliseconds)
    // User input arrived just before the screensaver timer would have fired.
    machine.processEvent(UserInput)

    delay(50.milliseconds)
    check(home.isActive) { "user input should have reset the timer; home should still be active" }

    delay(50.milliseconds)
    check(screensaver.isActive) { "screensaver should fire after timer restart" }
    println("Done")
}
