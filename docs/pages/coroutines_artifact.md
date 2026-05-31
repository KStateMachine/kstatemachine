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

This page contains information about `kstatemachine-coroutines` artifact functionality,
which provides library APIs for working in the `Kotlin Coroutines` environment.

> This artifact depends on Kotlin Coroutines library

You can find common information about multithreading library usage and coroutines on
[multithreading page](https://kstatemachine.github.io/kstatemachine/pages/multithreading.html)

## Artifact separation

`KStateMachine` has first class support of coroutines. Even if you don't use `Kotlin Coroutines`
and `kstatemachine-coroutines` artifact all library callbacks are `suspendable` functions.
So the functionality of this module should not be treated as "wrappers" or "extensions".
This is just a core functionality which is separated from original `kstatemachine` artifact
to follow language architecture regarding coroutines support.

## State machine creation

The artifact contains `createStateMachine()` / `createStateMachineBlocking()` methods, which were described in
[Create state machine](https://kstatemachine.github.io/kstatemachine/pages/statemachine.html#create-state-machine)
block

## Flow notifications

Coroutines users often use `Flow` to react to changes from a source.
The library provides two `StateMachine` extensions that expose its notifications as flows:

* `activeStatesFlow()` returns a `StateFlow<Set<IState>>` that emits the current active-state set every time it changes.
  This is the primary API for driving UI updates reactively:

```kotlin
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.nsk.kstatemachine.statemachine.activeStatesFlow

machine.activeStatesFlow()
    .onEach { activeStates ->
        // runs on the machine's coroutine context — safe to read machine state here
        updateUi(activeStates)
    }
    .launchIn(uiScope)
```

* `stateMachineNotificationFlow()` returns a `SharedFlow` that emits every machine lifecycle event:
  started, stopped, destroyed, state entries/exits, and transition triggers/completions.
  Use it when you need finer-grained observation than `activeStatesFlow()` provides:

```kotlin
import ru.nsk.kstatemachine.statemachine.StateMachineNotification.*
import ru.nsk.kstatemachine.statemachine.stateMachineNotificationFlow

machine.stateMachineNotificationFlow(extraBufferCapacity = 10)
    .onEach { notification ->
        when (notification) {
            is TransitionTriggered -> println("Triggered by: ${notification.transitionParams.event}")
            is StateEntry -> println("Entered: ${notification.state.name}")
            is StateExit -> println("Exited: ${notification.state.name}")
            else -> Unit
        }
    }
    .launchIn(this)
```

{: .note }
`stateMachineNotificationFlow` accepts an `extraBufferCapacity` parameter. Set it high enough so that
fast producers do not drop notifications while downstream collectors are suspended.

See [FlowObservationSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/FlowObservationSample.kt)
for a complete runnable example.

## Event processing

The `kstatemachine-coroutines` artifact adds two non-suspending event dispatch functions on top of
the base `processEvent()` / `processEventBlocking()`:

* `processEventByLaunch()` — fire-and-forget. Dispatches the event in a new coroutine on the machine's scope
  and returns immediately with no result:

```kotlin
machine.processEventByLaunch(SomeEvent) // returns Unit; event is queued and processed asynchronously
```

* `processEventByAsync()` — non-suspending dispatch that returns a `Deferred<ProcessingResult>`.
  Useful when you need to verify how the event was handled without suspending at the call site:

```kotlin
val deferred = machine.processEventByAsync(SomeEvent)
// ... do other work ...
val result = deferred.await() // suspend here when you actually need the result
```

For a comparison of all four variants and when to choose each, see
[Choosing a processEvent variant](https://kstatemachine.github.io/kstatemachine/pages/events.html#choosing-a-processevent-variant)
on the Events page.

See [AsyncEventProcessingSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/AsyncEventProcessingSample.kt)
for a side-by-side runnable comparison of `processEventByLaunch` and `processEventByAsync`.