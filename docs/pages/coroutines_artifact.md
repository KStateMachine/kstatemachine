---
layout: page
title: Coroutines artifact
---

# Coroutines artifact
{: .no_toc }

## Page contents
{: .no_toc .text-delta }

- TOC
{:toc}

This page contains information about `kstatemachine-coroutines` artifact functionality.
Which contains the library APIs for working in `Kotlin Coroutines` environment.

> This artifact depends on Kotlin Coroutines library

You can find common information about multithreading library usage and coroutines on
[multithreading page](https://kstatemachine.github.io/kstatemachine/pages/multithreading.html)

## Artifact separation

`KStateMachine` has first class support of coroutines. Even if you don't use `Kotlin Coroutines`
and `kstatemachine-coroutines` artifact all library callbacks are `suspendable` functions. 
So the functionality of this module should not be treated as "wrappers" or "extensions". 
This is just a core functionality which is separated from original `kstatemachine` artifact
to fallow language architecture regarding coroutines support.

Contains additional functions to work with KStateMachine depending on Kotlin Coroutines library

##  State machine creation

The artifact contains `createStateMachine()` / `createStateMachineBlocking()` methods, which were described in 
[Create state machine](https://kstatemachine.github.io/kstatemachine/pages/statemachine.html#create-state-machine) 
block

## Flow notifications

Coroutines users often use `Flow` to get some changes from a source.
The library provides `StateMachine` extension methods which represents notification APIs (listeners) in a form of `Flow`:

* `stateMachineNotificationFlow()` returns a `SharedFlow` of all machine notifications:

```kotlin
 machine.stateMachineNotificationFlow().collect {
    when (it) {
        is Started -> println("Started ${it.machine}")
        is TransitionTriggered -> println("TransitionTriggered ${it.transitionParams.event}")
        is TransitionComplete -> println("TransitionComplete ${it.transitionParams.event}")
        is StateEntry -> println("StateEntry ${it.state}")
        is StateExit -> println("StateExit ${it.state}")
        is StateFinished -> println("StateFinished ${it.state}")
        is Stopped -> println("Stopped ${it.machine}")
        is Destroyed -> println("Destroyed ${it.machine}")
    }
}
```

* `activeStatesFlow()` returns a `StateFlow` of active machine states:

```kotlin
machine.activeStatesFlow().collect { activeStates ->
    println("The set of active states: $activeStates")
}
```

## Event processing

`processEventByLaunch()` and `processEventByAsync()` functions are described in 
[event processing](https://kstatemachine.github.io/kstatemachine/pages/events.html#event-processing) block.
You can use them to process events in asynchronous way.