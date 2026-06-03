/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.statemachine.processEventBlocking
import ru.nsk.kstatemachine.statemachine.restartBlocking

class JoinTransitionTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {

            "basic join fires when both regions reach join points" {
                lateinit var afterJoin: State
                lateinit var parallelState: State
                lateinit var jp1: IState
                lateinit var jp2: IState

                val machine = createTestStateMachine(coroutineStarterType) {
                    afterJoin = state("afterJoin")
                    parallelState = initialState("parallel", childMode = ChildMode.PARALLEL) {
                        state("region1") {
                            jp1 = state("jp1")
                            initialState("s1") {
                                transition<SwitchEvent> { targetState = jp1 }
                            }
                        }
                        state("region2") {
                            jp2 = state("jp2")
                            initialState("s2") {
                                transition<SwitchEventL1> { targetState = jp2 }
                            }
                        }
                        joinTransition(jp1, jp2, targetState = afterJoin)
                    }
                }

                machine.processEventBlocking(SwitchEvent)
                parallelState.isActive.shouldBeTrue()
                afterJoin.isActive.shouldBeFalse()

                machine.processEventBlocking(SwitchEventL1)
                afterJoin.isActive.shouldBeTrue()
                parallelState.isActive.shouldBeFalse()
            }

            "join fires regardless of region order" {
                lateinit var afterJoin: State
                lateinit var jp1: IState
                lateinit var jp2: IState

                val machine = createTestStateMachine(coroutineStarterType) {
                    afterJoin = state("afterJoin")
                    initialState("parallel", childMode = ChildMode.PARALLEL) {
                        state("region1") {
                            jp1 = state("jp1")
                            initialState("s1") {
                                transition<SwitchEvent> { targetState = jp1 }
                            }
                        }
                        state("region2") {
                            jp2 = state("jp2")
                            initialState("s2") {
                                transition<SwitchEventL1> { targetState = jp2 }
                            }
                        }
                        joinTransition(jp1, jp2, targetState = afterJoin)
                    }
                }

                // Region2 joins first this time
                machine.processEventBlocking(SwitchEventL1)
                afterJoin.isActive.shouldBeFalse()

                machine.processEventBlocking(SwitchEvent)
                afterJoin.isActive.shouldBeTrue()
            }

            "join does not fire while only one region has joined" {
                lateinit var parallelState: State
                lateinit var jp1: IState
                lateinit var jp2: IState
                lateinit var afterJoin: State

                val machine = createTestStateMachine(coroutineStarterType) {
                    afterJoin = state("afterJoin")
                    parallelState = initialState("parallel", childMode = ChildMode.PARALLEL) {
                        state("region1") {
                            jp1 = state("jp1")
                            initialState("s1") {
                                transition<SwitchEvent> { targetState = jp1 }
                            }
                        }
                        state("region2") {
                            jp2 = state("jp2")
                            initialState("s2") {
                                transition<SwitchEventL1> { targetState = jp2 }
                            }
                        }
                        joinTransition(jp1, jp2, targetState = afterJoin)
                    }
                }

                machine.processEventBlocking(SwitchEvent)
                jp1.isActive.shouldBeTrue()
                parallelState.isActive.shouldBeTrue()
                afterJoin.isActive.shouldBeFalse()
            }

            "soft blocking: joined region ignores subsequent events" {
                lateinit var jp1: IState
                lateinit var jp2: IState
                lateinit var afterJoin: State

                val machine = createTestStateMachine(coroutineStarterType) {
                    afterJoin = state("afterJoin")
                    initialState("parallel", childMode = ChildMode.PARALLEL) {
                        state("region1") {
                            jp1 = state("jp1")
                            initialState("s1") {
                                transition<SwitchEvent> { targetState = jp1 }
                            }
                        }
                        state("region2") {
                            jp2 = state("jp2")
                            initialState("s2") {
                                transition<SwitchEventL1> { targetState = jp2 }
                            }
                        }
                        joinTransition(jp1, jp2, targetState = afterJoin)
                    }
                }

                machine.processEventBlocking(SwitchEvent)
                jp1.isActive.shouldBeTrue()

                // A second SwitchEvent arrives; jp1 has no transitions so region1 is blocked
                machine.processEventBlocking(SwitchEvent)
                jp1.isActive.shouldBeTrue()  // unchanged
                afterJoin.isActive.shouldBeFalse()
            }

            "join transition carries correct targetState" {
                lateinit var jp1: IState
                lateinit var jp2: IState
                lateinit var afterJoin: State

                val machine = createTestStateMachine(coroutineStarterType) {
                    afterJoin = state("afterJoin")
                    initialState("parallel", childMode = ChildMode.PARALLEL) {
                        state("region1") {
                            jp1 = state("jp1")
                            initialState("s1") {
                                transition<SwitchEvent> { targetState = jp1 }
                            }
                        }
                        state("region2") {
                            jp2 = state("jp2")
                            initialState("s2") {
                                transition<SwitchEventL1> { targetState = jp2 }
                            }
                        }
                        joinTransition(jp1, jp2, targetState = afterJoin)
                    }
                }

                machine.processEventBlocking(SwitchEvent)
                machine.processEventBlocking(SwitchEventL1)

                afterJoin.isActive.shouldBeTrue()
                machine.activeStates().containsAll(setOf(afterJoin)).shouldBeTrue()
            }

            "machine restart: join fires correctly on second run" {
                lateinit var jp1: IState
                lateinit var jp2: IState
                lateinit var afterJoin: State

                val machine = createTestStateMachine(coroutineStarterType) {
                    afterJoin = state("afterJoin")
                    initialState("parallel", childMode = ChildMode.PARALLEL) {
                        state("region1") {
                            jp1 = state("jp1")
                            initialState("s1") {
                                transition<SwitchEvent> { targetState = jp1 }
                            }
                        }
                        state("region2") {
                            jp2 = state("jp2")
                            initialState("s2") {
                                transition<SwitchEventL1> { targetState = jp2 }
                            }
                        }
                        joinTransition(jp1, jp2, targetState = afterJoin)
                    }
                }

                machine.processEventBlocking(SwitchEvent)
                machine.processEventBlocking(SwitchEventL1)
                afterJoin.isActive.shouldBeTrue()

                machine.restartBlocking()

                machine.processEventBlocking(SwitchEvent)
                afterJoin.isActive.shouldBeFalse()

                machine.processEventBlocking(SwitchEventL1)
                afterJoin.isActive.shouldBeTrue()
            }

            "multiple joins on same machine are independent" {
                lateinit var jp1a: IState
                lateinit var jp2a: IState
                lateinit var jp1b: IState
                lateinit var jp2b: IState
                lateinit var afterJoinA: State
                lateinit var afterJoinB: State

                val machine = createTestStateMachine(coroutineStarterType) {
                    afterJoinA = state("afterJoinA")
                    afterJoinB = state("afterJoinB")

                    initialState("parallelA", childMode = ChildMode.PARALLEL) {
                        state("regionA1") {
                            jp1a = state("jp1a")
                            initialState("sA1") {
                                transition<SwitchEvent> { targetState = jp1a }
                            }
                        }
                        state("regionA2") {
                            jp2a = state("jp2a")
                            initialState("sA2") {
                                transition<SwitchEventL1> { targetState = jp2a }
                            }
                        }
                        joinTransition(jp1a, jp2a, name = "joinA", targetState = afterJoinA)
                    }
                }

                // Only parallelA in this machine — afterJoinB is in a separate machine below
                val machine2 = createTestStateMachine(coroutineStarterType) {
                    afterJoinB = state("afterJoinB")
                    initialState("parallelB", childMode = ChildMode.PARALLEL) {
                        state("regionB1") {
                            jp1b = state("jp1b")
                            initialState("sB1") {
                                transition<SwitchEvent> { targetState = jp1b }
                            }
                        }
                        state("regionB2") {
                            jp2b = state("jp2b")
                            initialState("sB2") {
                                transition<SwitchEventL1> { targetState = jp2b }
                            }
                        }
                        joinTransition(jp1b, jp2b, name = "joinB", targetState = afterJoinB)
                    }
                }

                machine.processEventBlocking(SwitchEvent)
                machine2.processEventBlocking(SwitchEvent)

                // Neither join fired yet
                afterJoinA.isActive.shouldBeFalse()
                afterJoinB.isActive.shouldBeFalse()

                machine.processEventBlocking(SwitchEventL1)
                afterJoinA.isActive.shouldBeTrue()   // machine A joined
                afterJoinB.isActive.shouldBeFalse()  // machine B not yet

                machine2.processEventBlocking(SwitchEventL1)
                afterJoinB.isActive.shouldBeTrue()   // machine B joined
            }

            "joinTransition requires ChildMode.PARALLEL" {
                shouldThrow<IllegalStateException> {
                    createTestStateMachine(coroutineStarterType) {
                        val jp1 = state("jp1")
                        val jp2 = state("jp2")
                        initialState("exclusive") {
                            joinTransition(jp1, jp2, targetState = state("target"))
                        }
                    }
                }
            }

            "joinTransition requires at least 2 join points" {
                shouldThrow<IllegalArgumentException> {
                    createTestStateMachine(coroutineStarterType) {
                        val jp1 = state("jp1")
                        initialState("parallel", childMode = ChildMode.PARALLEL) {
                            state("r1") { initialState("s1") }
                            joinTransition(jp1, targetState = state("target"))
                        }
                    }
                }
            }

            "join with 3 regions" {
                lateinit var jp1: IState
                lateinit var jp2: IState
                lateinit var jp3: IState
                lateinit var afterJoin: State

                val machine = createTestStateMachine(coroutineStarterType) {
                    afterJoin = state("afterJoin")
                    initialState("parallel", childMode = ChildMode.PARALLEL) {
                        state("region1") {
                            jp1 = state("jp1")
                            initialState("s1") {
                                transition<SwitchEvent> { targetState = jp1 }
                            }
                        }
                        state("region2") {
                            jp2 = state("jp2")
                            initialState("s2") {
                                transition<SwitchEventL1> { targetState = jp2 }
                            }
                        }
                        state("region3") {
                            jp3 = state("jp3")
                            initialState("s3") {
                                transition<SwitchEventL2> { targetState = jp3 }
                            }
                        }
                        joinTransition(jp1, jp2, jp3, targetState = afterJoin)
                    }
                }

                machine.processEventBlocking(SwitchEvent)
                afterJoin.isActive.shouldBeFalse()

                machine.processEventBlocking(SwitchEventL1)
                afterJoin.isActive.shouldBeFalse()

                machine.processEventBlocking(SwitchEventL2)
                afterJoin.isActive.shouldBeTrue()
            }
        }
    }
})
