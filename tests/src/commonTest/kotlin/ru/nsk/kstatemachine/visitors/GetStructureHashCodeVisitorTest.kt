/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.visitors

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.QueuePendingEventHandler
import ru.nsk.kstatemachine.statemachine.buildCreationArguments
import ru.nsk.kstatemachine.statemachine.queuePendingEventHandler
import ru.nsk.kstatemachine.statemachine.throwingPendingEventHandler
import ru.nsk.kstatemachine.transition.TransitionType

class GetStructureHashCodeVisitorTest : StringSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "structure hash code should be the same if called twice" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState()
            }
            val hashCode = machine.structureHashCode
            hashCode shouldNotBe 0
            hashCode shouldBe machine.structureHashCode
        }

        "structure hash code for different StateMachine instances should be the same if they have equal structure" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState()
            }

            val machine2 = createTestStateMachine(coroutineStarterType) {
                initialState()
            }

            val machine3 = createTestStateMachine(coroutineStarterType) {
                initialState("State")
            }
            val hashCode = machine.structureHashCode
            hashCode shouldBe machine2.structureHashCode
            hashCode shouldNotBe machine3.structureHashCode
        }

        "structure hash code should catch state reorder (this might affect behaviour in some corner cases)" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState("state1")
                state("state2")
            }

            val machine2 = createTestStateMachine(coroutineStarterType) {
                state("state2")
                initialState("state1")
            }

            machine.structureHashCode shouldNotBe machine2.structureHashCode
        }

        "negative structure hash code should catch state reorder (not works for empty states)" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState()
                state()
            }

            val machine2 = createTestStateMachine(coroutineStarterType) {
                state()
                initialState()
            }

            // should not be equal, but cannot implement it
            machine.structureHashCode shouldBe machine2.structureHashCode
        }

        "structure hash code is affected by CreationArguments" {
            val machine = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments { isUndoEnabled = false }
            ) {
                initialState()
            }

            val machine2 = createTestStateMachine(
                coroutineStarterType,
                creationArguments = buildCreationArguments { isUndoEnabled = true }
            ) {
                initialState()
            }

            machine.structureHashCode shouldNotBe machine2.structureHashCode
        }

        "structure hash code is affected by state name" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState("state1")
            }

            val machine2 = createTestStateMachine(coroutineStarterType) {
                initialState("state1_2")
            }

            machine.structureHashCode shouldNotBe machine2.structureHashCode
        }

        "structure hash code is affected by state count" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState()
                state()
                state()
            }

            val machine2 = createTestStateMachine(coroutineStarterType) {
                initialState()
                state()
            }

            machine.structureHashCode shouldNotBe machine2.structureHashCode
        }

        "structure hash code is affected by state type" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState()
            }

            val machine2 = createTestStateMachine(coroutineStarterType) {
                initialDataState<Int>(defaultData = 0)
            }

            machine.structureHashCode shouldNotBe machine2.structureHashCode
        }

        "structure hash code is affected by generic DataState type" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialDataState<Double>(defaultData = 0.0)
            }

            val machine2 = createTestStateMachine(coroutineStarterType) {
                initialDataState<Int>(defaultData = 0)
            }

            machine.structureHashCode shouldNotBe machine2.structureHashCode
        }

        "structure hash code is affected by DataState defaultData" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialDataState<Int>(defaultData = 0)
            }

            val machine2 = createTestStateMachine(coroutineStarterType) {
                initialDataState<Int>(defaultData = 1)
            }

            machine.structureHashCode shouldNotBe machine2.structureHashCode
        }

        "structure hash code is affected by transition" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState {
                    transition<SwitchEvent>()
                }
            }

            val machine2 = createTestStateMachine(coroutineStarterType) {
                initialState()
            }

            machine.structureHashCode shouldNotBe machine2.structureHashCode
        }

        "structure hash code is affected by transition name" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState {
                    transition<SwitchEvent>("transition1")
                }
            }

            val machine2 = createTestStateMachine(coroutineStarterType) {
                initialState {
                    transition<SwitchEvent>("transition1_2")
                }
            }

            machine.structureHashCode shouldNotBe machine2.structureHashCode
        }

        "structure hash code is affected by transition event" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState {
                    transition<FirstEvent>()
                }
            }

            val machine2 = createTestStateMachine(coroutineStarterType) {
                initialState {
                    transition<SecondEvent>()
                }
            }

            machine.structureHashCode shouldNotBe machine2.structureHashCode
        }

        "structure hash code is affected by transition count" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState {
                    transition<SwitchEvent>()
                    transition<SwitchEvent>()
                }
            }

            val machine2 = createTestStateMachine(coroutineStarterType) {
                initialState {
                    transition<SwitchEvent>()
                }
            }

            machine.structureHashCode shouldNotBe machine2.structureHashCode
        }

        "structure hash code is affected by transition type" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState {
                    transition<SwitchEvent>()
                }
            }

            val machine2 = createTestStateMachine(coroutineStarterType) {
                initialState {
                    transition<SwitchEvent>(type = TransitionType.EXTERNAL)
                }
            }

            machine.structureHashCode shouldNotBe machine2.structureHashCode
        }

        "structure hash code is affected by child mode" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState()
            }

            val machine2 = createTestStateMachine(coroutineStarterType, childMode = ChildMode.PARALLEL) {
                state()
            }

            machine.structureHashCode shouldNotBe machine2.structureHashCode
        }

        "structure hash code is affected by HistoryType" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState()
                historyState(historyType = HistoryType.DEEP)
            }

            val machine2 = createTestStateMachine(coroutineStarterType) {
                initialState()
                historyState(historyType = HistoryType.SHALLOW)
            }

            machine.structureHashCode shouldNotBe machine2.structureHashCode
        }

        "structure hash code is affected by ${QueuePendingEventHandler::class.simpleName}" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState()
                pendingEventHandler = queuePendingEventHandler()
            }

            val machine2 = createTestStateMachine(coroutineStarterType) {
                initialState()
                pendingEventHandler = throwingPendingEventHandler()
            }

            machine.structureHashCode shouldNotBe machine2.structureHashCode
        }
    }
})
