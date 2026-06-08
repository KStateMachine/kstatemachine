/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.types.shouldBeInstanceOf
import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.event.DataEvent
import ru.nsk.kstatemachine.event.StartEvent
import ru.nsk.kstatemachine.event.UndoEvent
import ru.nsk.kstatemachine.event.WrappedEvent
import ru.nsk.kstatemachine.state.UndoTestData.SwitchDataEvent
import ru.nsk.kstatemachine.statemachine.*
import ru.nsk.kstatemachine.statemachine.StateMachine.IgnoredEventHandler
import ru.nsk.kstatemachine.statemachine.StateMachine.Logger
import ru.nsk.kstatemachine.transition.targetParallelStates
import ru.nsk.kstatemachine.transition.unwrappedArgument
import ru.nsk.kstatemachine.transition.unwrappedEvent

private object UndoTestData {
    class SwitchDataEvent(override val data: Int) : DataEvent<Int>
}

class UndoTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "undo not enabled" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState()
                }
                shouldThrowWithMessage<IllegalStateException>(
                    "Undo functionality is not enabled, use createStateMachine(creationArguments = CreationArguments(isUndoEnabled = true)) argument to enable it."
                ) { machine.undo() }
            }

            "undo throws with throwing PendingEventHandler" {
                lateinit var state1: State
                lateinit var state2: State
                val machine = createTestStateMachine(
                    coroutineStarterType,
                    creationArguments = buildCreationArguments { isUndoEnabled = true }
                ) {
                    pendingEventHandler = throwingPendingEventHandler()
                    state1 = initialState("state1") {
                        transitionOn<SwitchEvent> { targetState = { state2 } }
                    }
                    state2 = state("state2") {
                        onEntry {
                            shouldThrow<IllegalStateException> {
                                machine.undo()
                            }.message shouldEndWith "Do not call processEvent() from notification listeners or use queuePendingEventHandler()"
                        }
                    }
                }

                machine.activeStates() shouldContain state1
                machine.processEvent(SwitchEvent)
                machine.activeStates() shouldContain state2
            }

            "undo with QueuePendingEventHandler" {
                lateinit var state1: State
                lateinit var state2: State
                val machine = createTestStateMachine(
                    coroutineStarterType,
                    creationArguments = buildCreationArguments { isUndoEnabled = true }
                ) {
                    state1 = initialState("state1") {
                        transitionOn<SwitchEvent> { targetState = { state2 } }
                    }
                    state2 = state("state2") {
                        onEntry { machine.undo() }
                    }
                }
                machine.processEvent(SwitchEvent)
                machine.activeStates().shouldContain(state1)
            }

            "undo to initial state" {
                lateinit var state1: State
                lateinit var state2: State
                val machine = createTestStateMachine(
                    coroutineStarterType,
                    creationArguments = buildCreationArguments { isUndoEnabled = true }
                ) {
                    state1 = initialState("state1") {
                        transitionOn<SwitchEvent> { targetState = { state2 } }
                    }
                    state2 = state("state2")
                }

                machine.activeStates() shouldContain state1
                machine.processEvent(SwitchEvent)
                machine.activeStates() shouldContain state2
                machine.undo()
                machine.activeStates() shouldContain state1
            }

            "undo to initial state checking events and calling undo on initial state" {
                lateinit var state1: State
                lateinit var state2: State
                val machine = createTestStateMachine(
                    coroutineStarterType,
                    creationArguments = buildCreationArguments { isUndoEnabled = true }
                ) {
                    state1 = initialState("state1") {
                        transitionOn<FirstEvent> { targetState = { state2 } }
                    }
                    state2 = state("state2") {
                        transitionOn<SecondEvent> { targetState = { state1 } }
                    }
                }
                machine.undo() // does nothing, and should not break anything

                machine.activeStates() shouldContain state1
                machine.processEvent(FirstEvent)
                machine.processEvent(SecondEvent)
                machine.processEvent(FirstEvent)
                machine.activeStates() shouldContain state2

                state1.onEntry(once = true) { it.unwrappedEvent shouldBe SecondEvent }
                machine.undo()
                machine.activeStates() shouldContain state1

                state2.onEntry(once = true) { it.unwrappedEvent shouldBe FirstEvent }
                machine.undo()
                machine.activeStates() shouldContain state2

                state1.onEntry(once = true) { it.unwrappedEvent.shouldBeInstanceOf<StartEvent>() }
                machine.undo()
                machine.activeStates() shouldContain state1
            }

            "undo mixed with processEvent()" {
                lateinit var state1: State
                lateinit var state2: State
                val machine = createTestStateMachine(
                    coroutineStarterType,
                    creationArguments = buildCreationArguments { isUndoEnabled = true }
                ) {
                    state1 = initialState("state1") {
                        transitionOn<FirstEvent> { targetState = { state2 } }
                    }
                    state2 = state("state2") {
                        transitionOn<SecondEvent> { targetState = { state1 } }
                    }
                }

                machine.activeStates() shouldContain state1
                machine.processEvent(FirstEvent)
                machine.activeStates() shouldContain state2

                state1.onEntry(once = true) { it.unwrappedEvent.shouldBeInstanceOf<StartEvent>() }
                machine.undo()
                machine.activeStates() shouldContain state1

                // again
                machine.processEvent(FirstEvent)
                machine.activeStates() shouldContain state2
                machine.processEvent(SecondEvent)
                machine.activeStates() shouldContain state1

                state2.onEntry { it.unwrappedEvent shouldBe FirstEvent }
                machine.undo() // SecondEvent
                machine.activeStates() shouldContain state2

                state1.onEntry { it.unwrappedEvent.shouldBeInstanceOf<StartEvent>() }
                machine.undo() // FirstEvent
                machine.activeStates() shouldContain state1
            }

            "undo multiple times" {
                lateinit var state1: State
                lateinit var state2: State
                lateinit var state3: State
                val machine = createTestStateMachine(
                    coroutineStarterType,
                    creationArguments = buildCreationArguments { isUndoEnabled = true }
                ) {
                    state1 = initialState("state1") {
                        transitionOn<SwitchEvent> { targetState = { state2 } }
                    }
                    state2 = state("state2") {
                        transitionOn<SwitchEvent> { targetState = { state3 } }
                    }
                    state3 = state("state3")
                }

                machine.activeStates() shouldContain state1
                machine.processEvent(SwitchEvent)
                machine.activeStates() shouldContain state2
                machine.processEvent(SwitchEvent)
                machine.activeStates() shouldContain state3
                machine.undo()
                machine.activeStates() shouldContain state2
                machine.undo()
                machine.activeStates() shouldContain state1
            }

            "undo cross-level transition" {
                lateinit var state12: State
                lateinit var state2: State
                val machine = createTestStateMachine(
                    coroutineStarterType,
                    creationArguments = buildCreationArguments { isUndoEnabled = true }
                ) {
                    initialState("state1") {
                        initialState("state11") {
                            transitionOn<SwitchEvent> { targetState = { state12 } }
                        }
                        state12 = state("state12") {
                            transitionOn<SwitchEvent> { targetState = { state2 } }
                        }
                    }
                    state2 = state("state2")
                }
                machine.processEvent(SwitchEvent)
                machine.activeStates().shouldContain(state12)
                machine.processEvent(SwitchEvent)
                machine.activeStates().shouldContain(state2)
                machine.processEvent(UndoEvent) // alternative syntax
                machine.activeStates().shouldContain(state12)
            }

            "single undo with DataState" {
                lateinit var state12: DataState<Int>
                lateinit var state2: State
                val machine = createTestStateMachine(
                    coroutineStarterType,
                    creationArguments = buildCreationArguments { isUndoEnabled = true }
                ) {
                    initialState("state1") {
                        initialState("state11") {
                            dataTransitionOn<SwitchDataEvent, Int> { targetState = { state12 } }
                        }
                        state12 = dataState("state12") {
                            transitionOn<SwitchEvent> { targetState = { state2 } }
                        }
                    }
                    state2 = state("state2")
                }

                machine.processEvent(SwitchDataEvent(42))
                state12.data shouldBe 42
                machine.processEvent(SwitchEvent)
                machine.activeStates().shouldContain(state2)
                shouldThrowWithMessage<IllegalStateException>(
                    "Data is not set. Is DefaultDataState(state12) state active?"
                ) { state12.data }
                machine.undo()
                machine.activeStates().shouldContain(state12)
                state12.data shouldBe 42
            }

            "multiple undo with DataState" {
                lateinit var state11: State
                lateinit var state12: DataState<Int>
                lateinit var state2: State
                val machine = createTestStateMachine(
                    coroutineStarterType,
                    creationArguments = buildCreationArguments { isUndoEnabled = true }
                ) {
                    logger = Logger { println(it()) }
                    initialState("state1") {
                        state11 = initialState("state11") {
                            dataTransitionOn<SwitchDataEvent, Int> { targetState = { state12 } }
                            transitionOn<SecondEvent> { targetState = { state2 } }
                        }
                        state12 = dataState("state12") {
                            transitionOn<FirstEvent> { targetState = { state11 } }
                        }
                    }
                    state2 = state("state2")
                }

                val iterations = 3
                for (iteration in 1..iterations) {
                    machine.processEvent(SwitchDataEvent(iteration))
                    machine.processEvent(FirstEvent)
                }
                machine.processEvent(SecondEvent)

                machine.undo()
                machine.activeStates().shouldContain(state11)

                for (iteration in 3 downTo iterations) {
                    machine.undo()
                    machine.activeStates().shouldContain(state12)
                    state12.data shouldBe iteration

                    machine.undo()
                    machine.activeStates().shouldContain(state11)
                }
            }

            "undo self targeted transitions" {
                lateinit var state1: State
                lateinit var state2: State

                val machine = createTestStateMachine(
                    coroutineStarterType,
                    creationArguments = buildCreationArguments { isUndoEnabled = true }
                ) {
                    state1 = initialState("state1") {
                        transitionOn<SwitchEvent> { targetState = { state2 } }
                    }
                    state2 = state("state2") {
                        transition<SwitchEvent>()
                    }
                    onTransitionTriggered {
                        println("transition event: ${it.event}, argument: ${it.argument}")
                    }
                }

                state2.onEntry(once = true) { it.argument shouldBe 0 }
                machine.processEvent(SwitchEvent, 0)
                machine.activeStates().shouldContain(state2)

                machine.processEvent(SwitchEvent)
                machine.processEvent(SwitchEvent)
                machine.activeStates().shouldContain(state2)
                machine.undo()
                machine.activeStates().shouldContain(state2)
                machine.undo()
                machine.activeStates().shouldContain(state2)
                machine.undo()
                machine.activeStates().shouldContain(state1)
            }

            "undo initial state" {
                lateinit var state1: State
                lateinit var state2: State
                val machine = createTestStateMachine(
                    coroutineStarterType,
                    start = false,
                    creationArguments = buildCreationArguments { isUndoEnabled = true }
                ) {
                    state1 = initialState("state1") {
                        transitionOn<SwitchEvent> { targetState = { state2 } }
                    }
                    state2 = state("state2")
                }

                state1.onEntry(once = true) {
                    it.argument shouldBe 0
                    it.event.shouldBeInstanceOf<StartEvent>()
                }
                machine.start(0)

                machine.undo(1) // nothing
                machine.processEvent(SwitchEvent)

                state1.onEntry(once = true) {
                    it.argument shouldBe 2
                    it.unwrappedEvent.shouldBeInstanceOf<StartEvent>()
                }
                machine.undo(2)
            }

            "undo with argument, and unwrapped properties" {
                lateinit var state1: State
                lateinit var state2: State

                val machine = createTestStateMachine(
                    coroutineStarterType,
                    creationArguments = buildCreationArguments { isUndoEnabled = true },
                    start = false
                ) {
                    state1 = initialState("state1") {
                        transitionOn<SwitchEvent> { targetState = { state2 } }
                    }
                    state2 = state("state2")
                }

                state1.onEntry(once = true) {
                    it.event.shouldBeInstanceOf<StartEvent>()
                    it.argument shouldBe 1
                }
                machine.start(1)

                machine.processEvent(SwitchEvent)

                state1.onEntry(once = true) {
                    it.event.shouldBeInstanceOf<WrappedEvent>()
                    it.argument shouldBe 2
                    val wrappedEvent = it.event as WrappedEvent
                    wrappedEvent.event.shouldBeInstanceOf<StartEvent>()
                    it.unwrappedEvent shouldBe wrappedEvent.event
                    wrappedEvent.argument shouldBe 1
                    it.unwrappedArgument shouldBe wrappedEvent.argument
                }
                machine.processEvent(UndoEvent, 2)
            }

            "undo ignored event" {
                val machine = createTestStateMachine(
                    coroutineStarterType,
                    creationArguments = buildCreationArguments { isUndoEnabled = true }
                ) {
                    ignoredEventHandler = IgnoredEventHandler { throw TestException("test") }
                    initialState("state1") {
                        transition<SwitchEvent>()
                    }
                }
                machine.processEvent(SwitchEvent)
                machine.undo()
                shouldThrowWithMessage<TestException>("test") { machine.undo() }
            }

            "undo multi-target parallel transition restores all regions" {
                lateinit var state1: State
                lateinit var region1Initial: State
                lateinit var region1Other: State
                lateinit var region2Initial: State
                lateinit var region2Other: State
                lateinit var state3: State

                val machine = createTestStateMachine(
                    coroutineStarterType,
                    creationArguments = buildCreationArguments { isUndoEnabled = true }
                ) {
                    state1 = initialState("state1") {
                        transitionConditionally<SwitchEvent> {
                            direction = { targetParallelStates(region1Other, region2Other) }
                        }
                    }
                    state("parallelState", childMode = ChildMode.PARALLEL) {
                        state("region1") {
                            region1Initial = initialState("region1Initial")
                            region1Other = state("region1Other") {
                                transitionOn<SecondEvent> { targetState = { state3 } }
                            }
                        }
                        state("region2") {
                            region2Initial = initialState("region2Initial")
                            region2Other = state("region2Other")
                        }
                    }
                    state3 = state("state3")
                }

                machine.processEvent(SwitchEvent)
                machine.activeStates() shouldContain region1Other
                machine.activeStates() shouldContain region2Other

                machine.processEvent(SecondEvent)
                machine.activeStates() shouldContain state3

                machine.undo()
                machine.activeStates() shouldContain region1Other
                machine.activeStates() shouldContain region2Other
            }

            // The multi-target UndoState branch calls recursiveResolveTargetStates() for each
            // stored target, which includes the "initial pseudo-state" check. In practice the undo
            // stack always stores post-resolution leaf states (transitionParams.direction.targetStates
            // is produced by recursiveResolveTargetStates itself), so that check is always a no-op.
            // This test covers the end-to-end path: a parallel entry resolved through two choice
            // pseudo-states stores {leaf1, leaf2} in the stack, and undo restores exactly those leaves.
            "undo multi-target parallel transition where entry was resolved via initial choice states" {
                lateinit var parallelState: State
                lateinit var leaf1a: State
                lateinit var leaf2a: State
                lateinit var state3: State

                val machine = createTestStateMachine(
                    coroutineStarterType,
                    creationArguments = buildCreationArguments { isUndoEnabled = true }
                ) {
                    initialState("state1") {
                        transitionConditionally<SwitchEvent> {
                            // target the parallel composite — both choice states are resolved at entry time
                            direction = { targetState(parallelState) }
                        }
                    }
                    parallelState = state("parallelState", childMode = ChildMode.PARALLEL) {
                        // each region's initial is a choice pseudo-state → resolved on entry
                        state("region1") {
                            choiceState { leaf1a }  // always goes to leaf1a
                            leaf1a = state("leaf1a") {
                                transitionOn<SecondEvent> { targetState = { state3 } }
                            }
                        }
                        state("region2") {
                            choiceState { leaf2a }  // always goes to leaf2a
                            leaf2a = state("leaf2a")
                        }
                    }
                    state3 = state("state3")
                }

                machine.processEvent(SwitchEvent)
                // both choice states resolved → leaf1a and leaf2a active
                machine.activeStates() shouldContain leaf1a
                machine.activeStates() shouldContain leaf2a

                machine.processEvent(SecondEvent)
                machine.activeStates() shouldContain state3

                machine.undo()
                // multi-target undo restores both resolved leaves, not the composite parallelState
                machine.activeStates() shouldContain leaf1a
                machine.activeStates() shouldContain leaf2a
            }

            "undo from onEntry(), this uses event queue" {
                lateinit var state1: State
                lateinit var state2: State
                lateinit var state3: State

                val machine = createTestStateMachine(
                    coroutineStarterType,
                    creationArguments = buildCreationArguments { isUndoEnabled = true }
                ) {
                    state1 = initialState("state1") {
                        transitionOn<SwitchEvent> { targetState = { state2 } }
                    }
                    state2 = state("state2") {
                        onEntry {
                            it.unwrappedEvent shouldBe SwitchEvent
                        }

                        transitionOn<SwitchEvent> { targetState = { state3 } }
                    }
                    state3 = state("state3") {
                        onEntry { machine.undo() }
                    }
                }

                machine.processEvent(SwitchEvent)
                machine.processEvent(SwitchEvent)

                machine.undo() // same as machine.processEvent(UndoEvent)

                machine.activeStates() shouldContain state1
            }
        }
    }
})