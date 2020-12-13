package ru.nsk.kstatemachine

import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.then
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException

class NestedStateTest {
    @Test
    fun nestedState() {
        val callbacks = mock<Callbacks>()
        val inOrder = inOrder(callbacks)

        lateinit var firstL1: State
        lateinit var firstL2: State
        val firstL3 = object : DefaultState("firstL3") {}

        createStateMachine {
            firstL1 = initialState("firstL1") {
                onEntry { callbacks.onEntryState(this) }

                firstL2 = initialState("firstL2") {
                    onEntry { callbacks.onEntryState(this) }

                    addInitialState(firstL3) {
                        onEntry { callbacks.onEntryState(this) }
                    }
                }
            }
        }

        then(callbacks).should(inOrder).onEntryState(firstL1)
        then(callbacks).should(inOrder).onEntryState(firstL2)
        then(callbacks).should(inOrder).onEntryState(firstL3)
    }

    @Test
    fun nestedNoInitialState() {
        val machine = createStateMachine(start = false) {
            initialState("firstL1") {
                state("firstL2")
            }
        }

        shouldThrow<IllegalStateException> { machine.start() }
    }
}