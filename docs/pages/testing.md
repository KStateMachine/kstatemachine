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

You can enable internal state machine logging on your platform.

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