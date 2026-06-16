package ru.nsk.kstatemachine.transition

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import ru.nsk.kstatemachine.CoroutineStarterType
import ru.nsk.kstatemachine.SwitchEvent
import ru.nsk.kstatemachine.SwitchEventL1
import ru.nsk.kstatemachine.SwitchEventL2
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.state.ChildMode
import ru.nsk.kstatemachine.state.DataState
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.state.State
import ru.nsk.kstatemachine.state.activeStates
import ru.nsk.kstatemachine.state.dataState
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.joinDataTransition
import ru.nsk.kstatemachine.state.joinDataTransitionOn
import ru.nsk.kstatemachine.state.joinTransition
import ru.nsk.kstatemachine.state.joinTransitionConditionally
import ru.nsk.kstatemachine.state.joinTransitionOn
import ru.nsk.kstatemachine.state.state
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.statemachine.restart

class JoinTransitionTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {

            "basic joinTransition fires when both regions reach join points" {
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

                machine.processEvent(SwitchEvent)
                parallelState.isActive.shouldBeTrue()
                afterJoin.isActive.shouldBeFalse()

                machine.processEvent(SwitchEventL1)
                afterJoin.isActive.shouldBeTrue()
                parallelState.isActive.shouldBeFalse()
            }

            "basic scoped joinTransition fires when both regions reach join points" {
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
                        joinTransition {
                            joinStates = setOf(jp1, jp2)
                            targetState = afterJoin
                        }
                    }
                }

                machine.processEvent(SwitchEvent)
                parallelState.isActive.shouldBeTrue()
                afterJoin.isActive.shouldBeFalse()

                machine.processEvent(SwitchEventL1)
                afterJoin.isActive.shouldBeTrue()
                parallelState.isActive.shouldBeFalse()
            }

            "basic joinTransitionOn fires when both regions reach join points" {
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
                        joinTransitionOn {
                            joinStates = setOf(jp1, jp2)
                            targetState = { afterJoin }
                        }
                    }
                }

                machine.processEvent(SwitchEvent)
                parallelState.isActive.shouldBeTrue()
                afterJoin.isActive.shouldBeFalse()

                machine.processEvent(SwitchEventL1)
                afterJoin.isActive.shouldBeTrue()
                parallelState.isActive.shouldBeFalse()
            }

            "basic joinTransitionConditionally fires when both regions reach join points" {
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
                        joinTransitionConditionally {
                            joinStates = setOf(jp1, jp2)
                            direction = { targetState(afterJoin) }
                        }
                    }
                }

                machine.processEvent(SwitchEvent)
                parallelState.isActive.shouldBeTrue()
                afterJoin.isActive.shouldBeFalse()

                machine.processEvent(SwitchEventL1)
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
                machine.processEvent(SwitchEventL1)
                afterJoin.isActive.shouldBeFalse()

                machine.processEvent(SwitchEvent)
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

                machine.processEvent(SwitchEvent)
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

                machine.processEvent(SwitchEvent)
                jp1.isActive.shouldBeTrue()

                // A second SwitchEvent arrives; jp1 has no transitions so region1 is blocked
                machine.processEvent(SwitchEvent)
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

                machine.processEvent(SwitchEvent)
                machine.processEvent(SwitchEventL1)

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

                machine.processEvent(SwitchEvent)
                machine.processEvent(SwitchEventL1)
                afterJoin.isActive.shouldBeTrue()

                machine.restart()

                machine.processEvent(SwitchEvent)
                afterJoin.isActive.shouldBeFalse()

                machine.processEvent(SwitchEventL1)
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

                machine.processEvent(SwitchEvent)
                machine2.processEvent(SwitchEvent)

                // Neither join fired yet
                afterJoinA.isActive.shouldBeFalse()
                afterJoinB.isActive.shouldBeFalse()

                machine.processEvent(SwitchEventL1)
                afterJoinA.isActive.shouldBeTrue()   // machine A joined
                afterJoinB.isActive.shouldBeFalse()  // machine B not yet

                machine2.processEvent(SwitchEventL1)
                afterJoinB.isActive.shouldBeTrue()   // machine B joined
            }

            "joinTransition requires ChildMode.PARALLEL" {
                shouldThrow<IllegalArgumentException> {
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
                            joinTransition(jp1, jp1, targetState = state("target"))
                        }
                    }
                }
            }

            "joinDataTransition feeds computed data to DataState target" {
                lateinit var afterJoin: DataState<String>
                lateinit var jp1: IState
                lateinit var jp2: IState

                val machine = createTestStateMachine(coroutineStarterType) {
                    afterJoin = dataState("afterJoin")
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
                        joinDataTransition {
                            joinStates = setOf(jp1, jp2)
                            targetState = afterJoin
                            dataProducer = { "joined" }
                        }
                    }
                }

                machine.processEvent(SwitchEvent)
                afterJoin.isActive.shouldBeFalse()

                machine.processEvent(SwitchEventL1)
                afterJoin.isActive.shouldBeTrue()
                afterJoin.data shouldBe "joined"
            }

            "scoped joinDataTransition feeds computed data to DataState target" {
                lateinit var afterJoin: DataState<String>
                lateinit var jp1: IState
                lateinit var jp2: IState

                val machine = createTestStateMachine(coroutineStarterType) {
                    afterJoin = dataState("afterJoin")
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
                        joinDataTransition {
                            joinStates = setOf(jp1, jp2)
                            targetState = afterJoin
                            dataProducer = { "joined" }
                        }
                    }
                }

                machine.processEvent(SwitchEvent)
                afterJoin.isActive.shouldBeFalse()

                machine.processEvent(SwitchEventL1)
                afterJoin.isActive.shouldBeTrue()
                afterJoin.data shouldBe "joined"
            }

            "joinDataTransitionOn feeds computed data to DataState target" {
                lateinit var afterJoin: DataState<String>
                lateinit var jp1: IState
                lateinit var jp2: IState

                val machine = createTestStateMachine(coroutineStarterType) {
                    afterJoin = dataState("afterJoin")
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
                        joinDataTransitionOn {
                            joinStates = setOf(jp1, jp2)
                            targetState = { afterJoin }
                            dataProducer = { "joined" }
                        }
                    }
                }

                machine.processEvent(SwitchEvent)
                afterJoin.isActive.shouldBeFalse()

                machine.processEvent(SwitchEventL1)
                afterJoin.isActive.shouldBeTrue()
                afterJoin.data shouldBe "joined"
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

                machine.processEvent(SwitchEvent)
                afterJoin.isActive.shouldBeFalse()

                machine.processEvent(SwitchEventL1)
                afterJoin.isActive.shouldBeFalse()

                machine.processEvent(SwitchEventL2)
                afterJoin.isActive.shouldBeTrue()
            }
        }
    }
})
