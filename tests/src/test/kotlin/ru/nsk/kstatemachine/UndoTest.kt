package ru.nsk.kstatemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ru.nsk.kstatemachine.UndoTestData.SwitchDataEvent

private object UndoTestData {
    class SwitchDataEvent(override val data: Int) : DataEvent<Int>
}

class UndoTest : StringSpec({
    CoroutineStarterType.values().forEach { coroutineStarterType ->
        "undo not enabled" {
            val machine = createTestStateMachine(coroutineStarterType) {
                initialState()
            }
            shouldThrow<IllegalStateException> { machine.undoCo() }
        }

        "undo throws with throwing PendingEventHandler" {
            lateinit var state1: State
            lateinit var state2: State
            val machine = createTestStateMachine(coroutineStarterType, enableUndo = true) {
                pendingEventHandler = throwingPendingEventHandler()
                state1 = initialState("state1") {
                    transitionOn<SwitchEvent> { targetState = { state2 } }
                }
                state2 = state("state2") {
                    onEntry { shouldThrow<IllegalStateException> { machine.undoCo() } }
                }
            }

            machine.activeStates() shouldContain state1
            machine.processEvent(SwitchEvent)
            machine.activeStates() shouldContain state2
        }

        "undo with QueuePendingEventHandler" {
            lateinit var state1: State
            lateinit var state2: State
            val machine = createTestStateMachine(coroutineStarterType, enableUndo = true) {
                state1 = initialState("state1") {
                    transitionOn<SwitchEvent> { targetState = { state2 } }
                }
                state2 = state("state2") {
                    onEntry { machine.undoCo() }
                }
            }
            machine.processEvent(SwitchEvent)
            machine.activeStates().shouldContain(state1)
        }

        "undo to initial state" {
            lateinit var state1: State
            lateinit var state2: State
            val machine = createTestStateMachine(coroutineStarterType, enableUndo = true) {
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
            val machine = createTestStateMachine(coroutineStarterType, enableUndo = true) {
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
            val machine = createTestStateMachine(coroutineStarterType, enableUndo = true) {
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
            val machine = createTestStateMachine(coroutineStarterType, enableUndo = true) {
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
            val machine = createTestStateMachine(coroutineStarterType, enableUndo = true) {
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
            val machine = createTestStateMachine(coroutineStarterType, enableUndo = true) {
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
            shouldThrow<IllegalStateException> { state12.data }
            machine.undo()
            machine.activeStates().shouldContain(state12)
            state12.data shouldBe 42
        }

        "multiple undo with DataState" {
            lateinit var state11: State
            lateinit var state12: DataState<Int>
            lateinit var state2: State
            val machine = createTestStateMachine(coroutineStarterType, enableUndo = true) {
                logger = StateMachine.Logger { println(it) }
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

            val machine = createTestStateMachine(coroutineStarterType, enableUndo = true) {
                state1 = initialState("state1") {
                    transitionOn<SwitchEvent> { targetState = { state2 } }
                }
                state2 = state("state2") {
                    transition<SwitchEvent>()
                }
                onTransition {
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
            val machine = createTestStateMachine(coroutineStarterType, start = false, enableUndo = true) {
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

            val machine = createTestStateMachine(coroutineStarterType, enableUndo = true, start = false) {
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
            val machine = createTestStateMachine(coroutineStarterType, enableUndo = true) {
                ignoredEventHandler = StateMachine.IgnoredEventHandler { throw TestException("test") }
                initialState("state1") {
                    transition<SwitchEvent>()
                }
            }
            machine.processEvent(SwitchEvent)
            machine.undo()
            shouldThrow<TestException> { machine.undo() }
        }

        "undo from onEntry(), this uses event queue" {
            lateinit var state1: State
            lateinit var state2: State
            lateinit var state3: State

            val machine = createTestStateMachine(coroutineStarterType, enableUndo = true) {
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
                    onEntry { machine.undoCo() }
                }
            }

            machine.processEvent(SwitchEvent)
            machine.processEvent(SwitchEvent)

            machine.undo() // same as machine.processEvent(UndoEvent)

            machine.activeStates() shouldContain state1
        }
    }
})