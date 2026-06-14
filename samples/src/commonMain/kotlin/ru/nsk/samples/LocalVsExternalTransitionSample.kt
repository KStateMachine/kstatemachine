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
import ru.nsk.kstatemachine.transition.TransitionType
import ru.nsk.samples.LocalVsExternalTransitionSample.SwitchEvent

private object LocalVsExternalTransitionSample {
    object SwitchEvent : Event
}

/**
 * [TransitionType.LOCAL] (default) and [TransitionType.EXTERNAL] differ when the transition
 * source and target share a common ancestor:
 * - LOCAL: the common ancestor is NOT exited and re-entered.
 * - EXTERNAL: the common ancestor IS exited and re-entered.
 *
 * In this example, both siblings (child1 → child2) live inside a parent state.
 * LOCAL leaves the parent untouched; EXTERNAL causes the parent to exit and re-enter.
 */
fun main() = runBlocking {
    var localParentEntries = 0
    var externalParentEntries = 0

    val localMachine = createStateMachine(this, "local") {
        logger = StateMachine.Logger { println("local: ${it()}") }
        initialState("parent") {
            onEntry { localParentEntries++ }
            val child2 = state("child2")
            initialState("child1") {
                transition<SwitchEvent> {
                    targetState = child2
                    type = TransitionType.LOCAL
                }
            }
        }
    }

    val externalMachine = createStateMachine(this, "external") {
        logger = StateMachine.Logger { println("external: ${it()}") }
        initialState("parent") {
            onEntry { externalParentEntries++ }
            val child2 = state("child2")
            initialState("child1") {
                transition<SwitchEvent> {
                    targetState = child2
                    type = TransitionType.EXTERNAL
                }
            }
        }
    }

    check(localParentEntries == 1)
    check(externalParentEntries == 1)

    localMachine.processEvent(SwitchEvent)
    externalMachine.processEvent(SwitchEvent)

    // LOCAL: parent was not re-entered
    check(localParentEntries == 1)
    // EXTERNAL: parent was exited and re-entered
    check(externalParentEntries == 2)

    println("LOCAL parent entries: $localParentEntries  (no re-entry)")
    println("EXTERNAL parent entries: $externalParentEntries  (re-entered)")
}
