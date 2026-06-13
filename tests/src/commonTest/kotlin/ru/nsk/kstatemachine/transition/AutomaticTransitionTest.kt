package ru.nsk.kstatemachine.transition

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import ru.nsk.kstatemachine.CoroutineStarterType
import ru.nsk.kstatemachine.SwitchEvent
import ru.nsk.kstatemachine.createTestStateMachine
import ru.nsk.kstatemachine.state.DataState
import ru.nsk.kstatemachine.state.State
import ru.nsk.kstatemachine.state.automaticDataTransition
import ru.nsk.kstatemachine.state.automaticTransition
import ru.nsk.kstatemachine.state.automaticTransitionConditionally
import ru.nsk.kstatemachine.state.automaticTransitionOn
import ru.nsk.kstatemachine.state.dataState
import ru.nsk.kstatemachine.state.initialState
import ru.nsk.kstatemachine.state.state
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.statemachine.processEventBlocking
import ru.nsk.kstatemachine.statemachine.restart
import ru.nsk.kstatemachine.transition.targetState

class AutomaticTransitionTest : FreeSpec({
    CoroutineStarterType.entries.forEach { coroutineStarterType ->
        "$coroutineStarterType" - {

            "eventless transition fires on entry" {
                lateinit var target: State
                val machine = createTestStateMachine(coroutineStarterType) {
                    target = state("target")
                    initialState("source") {
                        automaticTransition(targetState = target)
                    }
                }
                target.isActive.shouldBeTrue()
            }

            "chained eventless transitions A -> B -> C reach the leaf" {
                lateinit var b: State
                lateinit var c: State
                val machine = createTestStateMachine(coroutineStarterType) {
                    c = state("c")
                    b = state("b") {
                        automaticTransition("b->c", targetState = c)
                    }
                    initialState("a") {
                        automaticTransition("a->b", targetState = b)
                    }
                }
                c.isActive.shouldBeTrue()
                b.isActive.shouldBeFalse()
            }

            "guarded eventless transition stays put when guard rejects" {
                lateinit var source: State
                lateinit var target: State
                var allowed = false
                val machine = createTestStateMachine(coroutineStarterType) {
                    target = state("target")
                    source = initialState("source") {
                        automaticTransitionOn {
                            guard = { allowed }
                            targetState = { target }
                        }
                    }
                }
                source.isActive.shouldBeTrue()
                target.isActive.shouldBeFalse()

                allowed = true
                machine.restart()
                target.isActive.shouldBeTrue()
            }

            "conditional eventless transition picks the right branch" {
                lateinit var even: State
                lateinit var odd: State
                var counter = 0
                val machine = createTestStateMachine(coroutineStarterType) {
                    even = state("even")
                    odd = state("odd")
                    initialState("source") {
                        automaticTransitionConditionally {
                            direction = {
                                if (counter % 2 == 0) targetState(even) else targetState(odd)
                            }
                        }
                    }
                }
                even.isActive.shouldBeTrue()
                odd.isActive.shouldBeFalse()
            }

            "eventless data transition carries producer value into DataState" {
                lateinit var produced: DataState<Int>
                val machine = createTestStateMachine(coroutineStarterType) {
                    produced = dataState<Int>("produced")
                    initialState("source") {
                        automaticDataTransition(targetState = produced) { 42 }
                    }
                }
                produced.isActive.shouldBeTrue()
                produced.data shouldBe 42
            }

            "event-driven transition out of the same state still works when eventless guard rejects" {
                lateinit var source: State
                lateinit var auto: State
                lateinit var manual: State
                val machine = createTestStateMachine(coroutineStarterType) {
                    auto = state("auto")
                    manual = state("manual")
                    source = initialState("source") {
                        automaticTransitionOn {
                            guard = { false }
                            targetState = { auto }
                        }
                        transition<SwitchEvent>(targetState = manual)
                    }
                }
                source.isActive.shouldBeTrue()

                machine.processEventBlocking(SwitchEvent)
                manual.isActive.shouldBeTrue()
                auto.isActive.shouldBeFalse()
            }
        }
    }
})
