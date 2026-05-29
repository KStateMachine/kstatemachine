---
layout: page
title: Testing
---

# Testing
{: .no_toc }

## Page contents
{: .no_toc .text-delta }

- TOC
{:toc}

It is recommended to specify names for `States` and `Transitions`. This should help when someone tries to debug state
machine's behaviour.

## Logging

You can enable internal state machine logging on your platform. `StateMachine.Logger` is a `fun interface` whose
single method receives a **lazy** `() -> String` — the message is only evaluated if the logger actually uses it,
avoiding string construction overhead when logging is disabled.

On JVM:

```kotlin
createStateMachine(scope) {
    logger = StateMachine.Logger { lazyMessage ->
        println(lazyMessage())
    }
    // ...
}
```

On Android:

```kotlin
createStateMachine(scope) {
    logger = StateMachine.Logger { lazyMessage ->
        Log.d(this::class.qualifiedName, lazyMessage())
    }
    // ...
}
```

From inside a state callback you can write to the machine's logger via the `IState.log {}` extension:

```kotlin
state("myState") {
    onEntry { log { "Entered myState with event ${it.event}" } }
}
```

## Starting from particular state

For testing, it might be useful to check how state machine reacts on events from particular state. There
are several `Testing.startFrom()`/`Testing.startFromBlocking()` overloaded functions which allow starting the machine
from a specified state or states (for parallel regions):

```kotlin
lateinit var state2: State

val machine = createStateMachine(scope, start = false) {
    initialState("state1")
    state2 = state("state2")
    // ...
}

machine.startFrom(state2)
```

`startFromBlocking()` is the non-suspending equivalent for use in regular (non-coroutine) test code:

```kotlin
machine.startFromBlocking(state2)
```

To start in a `DataState` and inject initial data at the same time use the data overload:

```kotlin
lateinit var dataState: DataState<String>

val machine = createStateMachine(scope, start = false) {
    dataState = dataState<String>("loaded")
    // ...
}

machine.startFrom(dataState, data = "hello")
// or blocking:
machine.startFromBlocking(dataState, data = "hello")
```