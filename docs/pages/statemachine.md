---
layout: page
title: State machine
nav_order: 1
---

# State machine
{: .no_toc }

## Page contents
{: .no_toc .text-delta }

- TOC
{:toc}

## Create state machine

A state machine can be created with one of those factory functions:

* `createStateMachine()` suspendable version (from `kstatemachine-coroutines` artifact), the best choice by default.
* `createStateMachineBlocking()` blocking version (from `kstatemachine-coroutines` artifact)
* `createStdLibStateMachine()` - creates StateMachine instance without Kotlin Coroutines support
  (from `kstatemachine` artifact)

```kotlin
val machine = createStateMachine(
    scope, // 
    "Traffic lights" // Optional name is convenient for logging debugging and export
) {
    // Set up state machine ...
}
```

By default, factory functions start state machine. You can control it using `start` argument.
All overloads accept optional argument `CreationArguments` which allows to change some options.

Subsequent samples will use `createStateMachine()` function, but you can choose that one which fits your needs.

