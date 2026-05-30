/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.samples

import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine

private object ChoiceStateSample {
    var isFeatureEnabled = true
}

/**
 * [DefaultChoiceState] is a pseudo-state that immediately routes to another state based on a
 * runtime condition, without itself being entered or exited.
 * It is useful for conditional branching at machine startup or when entering a composite state.
 */
fun main() = runBlocking {
    val stateA: State
    val stateB: State

    val machine = createStateMachine(this) {
        logger = StateMachine.Logger { println(it()) }

        stateA = state("stateA") {
            onEntry { println("Entered stateA (feature enabled path)") }
        }
        stateB = state("stateB") {
            onEntry { println("Entered stateB (feature disabled path)") }
        }

        // choice pseudo-state: routing lambda runs immediately on entry
        initialChoiceState("choice") {
            if (ChoiceStateSample.isFeatureEnabled) stateA else stateB
        }
    }

    check(stateA in machine.activeStates())
    check(stateB !in machine.activeStates())
    println("Active: ${machine.activeStates().map { it.name }}")
}
