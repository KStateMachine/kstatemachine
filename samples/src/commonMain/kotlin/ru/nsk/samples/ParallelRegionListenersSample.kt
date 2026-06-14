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
import ru.nsk.samples.ParallelRegionListenersSample.FinishRegion1Event
import ru.nsk.samples.ParallelRegionListenersSample.FinishRegion2Event

private object ParallelRegionListenersSample {
    object FinishRegion1Event : Event
    object FinishRegion2Event : Event
}

/**
 * [onActiveAllOfBlocking] triggers its callback when ALL of the specified states become active simultaneously.
 * [onActiveAnyOfBlocking] triggers its callback when ANY of the specified states becomes active.
 * Both are useful for reacting to combinations of active states in parallel regions.
 */
fun main() = runBlocking {
    var anyOfActive = false
    var allOfActive = false

    val machine = createStateMachine(this, childMode = ChildMode.PARALLEL) {
        logger = StateMachine.Logger { println(it()) }

        val region1Done: IState
        val region2Done: IState

        val region1 = state("region1") {
            val done = finalState("region1Done")
            region1Done = done
            initialState("region1Active") {
                transition<FinishRegion1Event> { targetState = done }
            }
        }

        val region2 = state("region2") {
            val done = finalState("region2Done")
            region2Done = done
            initialState("region2Active") {
                transition<FinishRegion2Event> { targetState = done }
            }
        }

        onFinished { println("Both regions finished — machine complete") }

        // fires when either region1Done OR region2Done becomes active
        onActiveAnyOfBlocking(region1Done, region2Done) { active ->
            anyOfActive = active
            println("onActiveAnyOf changed: $active")
        }

        // fires when both region1Done AND region2Done are active simultaneously
        onActiveAllOfBlocking(region1Done, region2Done) { active ->
            allOfActive = active
            println("onActiveAllOf changed: $active")
        }
    }

    check(!anyOfActive)
    check(!allOfActive)

    machine.processEvent(FinishRegion1Event)
    check(anyOfActive)
    check(!allOfActive)

    machine.processEvent(FinishRegion2Event)
    check(anyOfActive)
    check(allOfActive)
    check(machine.isFinished)
}
