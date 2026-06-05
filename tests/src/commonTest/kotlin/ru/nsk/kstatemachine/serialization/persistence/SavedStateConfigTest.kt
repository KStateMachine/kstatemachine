/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.serialization.persistence

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import ru.nsk.kstatemachine.CoroutineStarterType
import ru.nsk.kstatemachine.SwitchEvent
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.event.DataEvent
import ru.nsk.kstatemachine.persistence.SavedStateConfig
import ru.nsk.kstatemachine.persistence.captureSavedStateConfig
import ru.nsk.kstatemachine.persistence.restoreBySavedStateConfig
import ru.nsk.kstatemachine.persistence.restoreBySavedStateConfigBlocking
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.*

private object SavedStateConfigTestData {
    class IntEvent(override val data: Int) : DataEvent<Int>
}

class SavedStateConfigTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {
            "capture and restore plain state" {
                lateinit var state2: State
                val machine1 = createTestStateMachine(coroutineStarterType, "machine") {
                    initialState("state1") {
                        transitionOn<SwitchEvent> { targetState = { state2 } }
                    }
                    state2 = state("state2")
                }
                machine1.processEvent(SwitchEvent)

                val snapshot = machine1.captureSavedStateConfig()
                snapshot.activeLeafStateNames shouldBe listOf("state2")

                lateinit var state2b: State
                val machine2 = createTestStateMachine(coroutineStarterType, "machine", start = false) {
                    initialState("state1") {
                        transitionOn<SwitchEvent> { targetState = { state2b } }
                    }
                    state2b = state("state2")
                }
                machine2.restoreBySavedStateConfig(snapshot)
                machine2.activeStates().single().name shouldBe "state2"
            }

            "capture and restore DataState preserves data" {
                lateinit var loaded: DataState<Int>
                suspend fun buildMachine() = createTestStateMachine(coroutineStarterType, "machine", start = false) {
                    initialState("idle") {
                        dataTransitionOn<SavedStateConfigTestData.IntEvent, Int> { targetState = { loaded } }
                    }
                    loaded = dataState("loaded")
                }

                val machine1 = buildMachine()
                machine1.start()
                machine1.processEvent(SavedStateConfigTestData.IntEvent(99))
                loaded.data shouldBe 99

                val snapshot = machine1.captureSavedStateConfig()
                snapshot.dataStateLastValues["loaded"] shouldBe 99

                val machine2 = buildMachine()
                machine2.restoreBySavedStateConfig(snapshot)
                (machine2.requireState("loaded") as DataState<*>).data shouldBe 99
            }

            "lastData of inactive DataState is captured and restored" {
                lateinit var loaded: DataState<Int>
                suspend fun buildMachine() = createTestStateMachine(coroutineStarterType, "machine", start = false) {
                    val idle = initialState("idle") {
                        dataTransitionOn<SavedStateConfigTestData.IntEvent, Int> { targetState = { loaded } }
                    }
                    loaded = dataState("loaded") {
                        transitionOn<SwitchEvent> { targetState = { idle } }
                    }
                }

                val machine1 = buildMachine()
                machine1.start()
                machine1.processEvent(SavedStateConfigTestData.IntEvent(42))
                loaded.data shouldBe 42
                machine1.processEvent(SwitchEvent) // back to idle, loaded is now inactive
                loaded.lastData shouldBe 42

                val snapshot = machine1.captureSavedStateConfig()
                snapshot.activeLeafStateNames shouldBe listOf("idle")
                snapshot.dataStateLastValues["loaded"] shouldBe 42

                val machine2 = buildMachine()
                machine2.restoreBySavedStateConfig(snapshot)
                (machine2.requireState("loaded") as DataState<*>).lastData shouldBe 42
            }

            "capture and restore MutableDataState preserves data" {
                lateinit var counter: MutableDataState<Int>
                suspend fun buildMachine() = createTestStateMachine(coroutineStarterType, "machine", start = false) {
                    counter = initialMutableDataState("counter", defaultData = 0)
                }

                val machine1 = buildMachine()
                machine1.start()
                counter.setData(42)

                val snapshot = machine1.captureSavedStateConfig()
                snapshot.dataStateLastValues["counter"] shouldBe 42

                val machine2 = buildMachine()
                machine2.restoreBySavedStateConfig(snapshot)
                (machine2.requireState("counter") as MutableDataState<*>).data shouldBe 42
            }

            "capture and restore parallel regions" {
                suspend fun buildMachine() = createTestStateMachine(coroutineStarterType, "machine", start = false) {
                    initialState("parallel", childMode = ChildMode.PARALLEL) {
                        state("p1")
                        state("p2")
                    }
                }

                val machine1 = buildMachine()
                machine1.start()
                val snapshot = machine1.captureSavedStateConfig()
                snapshot.activeLeafStateNames shouldContainExactlyInAnyOrder listOf("p1", "p2")

                val machine2 = buildMachine()
                machine2.restoreBySavedStateConfig(snapshot)
                machine2.activeStates().map { it.name } shouldContainExactlyInAnyOrder
                        listOf("parallel", "p1", "p2")
            }

            "structure hash mismatch throws" {
                val snapshot = SavedStateConfig(
                    structureHashCode = Int.MAX_VALUE,
                    activeLeafStateNames = listOf("state1"),
                    dataStateLastValues = emptyMap(),
                )
                val machine = createTestStateMachine(coroutineStarterType, "machine", start = false) {
                    initialState("state1")
                }
                shouldThrow<IllegalStateException> {
                    machine.restoreBySavedStateConfig(snapshot)
                }
            }

            "structure hash mismatch ignored when disabled" {
                val machine1 = createTestStateMachine(coroutineStarterType, "machine") {
                    initialState("state1")
                }
                val snapshot = machine1.captureSavedStateConfig()

                val machine2 = createTestStateMachine(coroutineStarterType, "machine", start = false) {
                    initialState("state1")
                    state("state2")
                }
                shouldNotThrowAny {
                    machine2.restoreBySavedStateConfig(snapshot, disableStructureHashCodeCheck = true)
                }
            }

            "capture on destroyed machine throws" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState("s1")
                }
                machine.destroy(stop = false)
                shouldThrowWithMessage<IllegalStateException>("$machine is already destroyed") {
                    machine.captureSavedStateConfig()
                }
            }

            "capture on stopped machine throws" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState("s1")
                }
                machine.stop()
                shouldThrow<IllegalStateException> {
                    machine.captureSavedStateConfig()
                }
            }

            "capture with isUndoEnabled throws" {
                val machine = createTestStateMachine(
                    coroutineStarterType,
                    creationArguments = buildCreationArguments { isUndoEnabled = true },
                ) {
                    initialState("s1") {
                        transition<SwitchEvent>("t") { targetState = this@initialState }
                    }
                }
                shouldThrow<IllegalStateException> {
                    machine.captureSavedStateConfig()
                }
            }

            "capture with isUndoEnabled succeeds when disableUndoEnabledCheck is true" {
                lateinit var state2: State
                suspend fun buildMachine() = createTestStateMachine(
                    coroutineStarterType,
                    "machine",
                    start = false,
                    creationArguments = buildCreationArguments { isUndoEnabled = true },
                ) {
                    initialState("state1") {
                        transitionOn<SwitchEvent> { targetState = { state2 } }
                    }
                    state2 = state("state2")
                }

                val machine1 = buildMachine()
                machine1.start()
                machine1.processEvent(SwitchEvent)

                val snapshot = shouldNotThrowAny {
                    machine1.captureSavedStateConfig(disableUndoEnabledCheck = true)
                }
                snapshot.activeLeafStateNames shouldBe listOf("state2")

                val machine2 = buildMachine()
                machine2.restoreBySavedStateConfig(snapshot)
                machine2.activeStates().single().name shouldBe "state2"
            }

            "capture with unnamed active state throws" {
                val machine = createTestStateMachine(coroutineStarterType) {
                    initialState() // no name
                }
                shouldThrow<IllegalStateException> {
                    machine.captureSavedStateConfig()
                }
            }

            "restore on already-processed machine throws" {
                val machine1 = createTestStateMachine(coroutineStarterType, "machine") {
                    initialState("state1")
                }
                val snapshot = machine1.captureSavedStateConfig()

                lateinit var state2: State
                val machine2 = createTestStateMachine(coroutineStarterType) {
                    initialState("state1") {
                        transitionOn<SwitchEvent> { targetState = { state2 } }
                    }
                    state2 = state("state2")
                }
                machine2.processEvent(SwitchEvent)

                shouldThrow<IllegalStateException> {
                    machine2.restoreBySavedStateConfig(snapshot)
                }
            }

            "restoration mutes listeners by default and fires them when muteListeners is false" {
                lateinit var state2: State
                suspend fun buildMachine(onEntry: () -> Unit) =
                    createTestStateMachine(coroutineStarterType, "machine", start = false) {
                        initialState("state1") {
                            transitionOn<SwitchEvent> { targetState = { state2 } }
                        }
                        state2 = state("state2") {
                            onEntry { onEntry() }
                        }
                    }

                val machine1 = buildMachine(onEntry = {})
                machine1.start()
                machine1.processEvent(SwitchEvent)
                val snapshot = machine1.captureSavedStateConfig()

                var mutedCount = 0
                val mutedMachine = buildMachine(onEntry = { mutedCount++ })
                mutedMachine.restoreBySavedStateConfig(snapshot) // muteListeners defaults to true
                mutedCount shouldBe 0

                var audibleCount = 0
                val audibleMachine = buildMachine(onEntry = { audibleCount++ })
                audibleMachine.restoreBySavedStateConfig(snapshot, muteListeners = false)
                audibleCount shouldBe 1
            }

            "restoreBySavedStateConfigBlocking works" {
                lateinit var state2: State
                val machine1 = createTestStateMachine(coroutineStarterType, "machine") {
                    initialState("state1") {
                        transitionOn<SwitchEvent> { targetState = { state2 } }
                    }
                    state2 = state("state2")
                }
                machine1.processEvent(SwitchEvent)

                val snapshot = machine1.captureSavedStateConfig()

                lateinit var state2b: State
                val machine2 = createTestStateMachine(coroutineStarterType, "machine", start = false) {
                    initialState("state1") {
                        transitionOn<SwitchEvent> { targetState = { state2b } }
                    }
                    state2b = state("state2")
                }
                machine2.restoreBySavedStateConfigBlocking(snapshot)
                machine2.activeStates().single().name shouldBe "state2"
            }
        }
    }
})
