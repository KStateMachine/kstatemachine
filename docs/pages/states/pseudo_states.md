---
layout: page
title: Pseudo states
parent: States
---

# Pseudo states
{: .no_toc }

## Page contents
{: .no_toc .text-delta }

- TOC
{:toc}

Pseudo states are special kind of states that machine cannot enter, but they are useful to describe additional
logic in machine behaviour.

### Choice state

Choice state allows to select target state depending on some condition. When transition targeting a choice state is
triggered, choice function is evaluated and machine goes to resulting state:

```kotlin
class SomeEvent(val value: Int) : Event

choiceState {
    when (event) {
        is SomeEvent -> { // cast is necessary as we don't know event type here
            if (someEvent.value > 3) State1 else State2
        }
        else -> { /*...*/
        }
    }
}
```

There is also `choiceDataState()` function available for choosing between `DataState`s of same type. You can
define `dataTransition`
to target such pseudo data state.

```kotlin
class IntEvent(override val data: Int) : DataEvent<Int>

lateinit var intState1: DataState<Int>
lateinit var intState2: DataState<Int>

createStateMachine(/*...*/) {
    // ...
    val choice = choiceDataState("data choice") {
        val intEvent = event as? IntEvent // cast is necessary as we don't know event type here
        if (intEvent?.data == 42) intState1 else intState2 // attempt of using state of other type will not compile
    }

    // here is a major reason to use choiceDataState(), to specify it as a target of dataTransition()
    dataTransition<IntEvent, Int> { targetState = choice }

    intState1 = dataState<Int>("intState1")
    intState2 = dataState<Int>("intState2")

}
```

You can use `choiceState` even on initial state branch.
Note that `choiceState` can not be active, so if the library performs a transition and finds that `choiceState` is
going to be activated, it executes its lambda argument and navigates to the resulting state.
If the resulting state is also a `PseudoState` instance, further redirections might be applied.

### History state

There are two types of history states, shallow and deep. Shallow history state is used to represent the most recently
active child (its neighbour) of a parent state. It does not recurse into this child's active configuration (sub states),
initial states will be used. Deep history state in contrast reflects the most recent active configuration of the parent
state (including all sub states).
You can specify default state which will be used if history was not recorded yet (parent was not active).
When default state is not specified, parent initial state will be entered on transition to history state.

```kotlin
val machine = createStateMachine(scope) {
    state {
        val state11 = initialState()
        val state12 = state()
        historyState(defultState = state12)
    }
    state {
        // ...
    }
}
```