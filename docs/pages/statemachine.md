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
Use `buildCreationArguments()` function to provide it.

Subsequent samples will use `createStateMachine()` function, but you can choose that one which fits your needs.

### Creation arguments

| Argument | Default | Description |
|---|---|---|
| `autoDestroyOnStatesReuse` | `true` | When a state owned by one machine is reused in another, automatically calls `destroy()` on the previous machine instead of throwing. |
| `isUndoEnabled` | `false` | Enables the undo transition. See [Undo transitions](https://kstatemachine.github.io/kstatemachine/pages/transitions/transitions.html#undo-transitions). |
| `doNotThrowOnMultipleTransitionsMatch` | `false` | When multiple transitions match an event, selects the first one instead of throwing. |
| `requireNonBlankNames` | `false` | Throws on machine start if any state or transition has a null or blank name. |
| `eventRecordingArguments` | `null` | Enables event recording for later persistence/restoration. See [Persistence](https://kstatemachine.github.io/kstatemachine/pages/persistence.html). |
| `skipCoroutineScopeValidityCheck` | `false` | Skips the check that warns when a multithreaded dispatcher (e.g. `Dispatchers.Default`) is used. See [Multithreading](https://kstatemachine.github.io/kstatemachine/pages/multithreading.html). |

```kotlin
val machine = createStateMachine(
    scope,
    creationArguments = buildCreationArguments {
        isUndoEnabled = true
        requireNonBlankNames = true
    }
) {
    // ...
}
```

## Lifecycle

A state machine can be stopped and restarted, or permanently destroyed.

```kotlin
machine.stop()          // suspendable; pauses the machine, transitions are not processed
machine.stopBlocking()  // blocking analog — do NOT call from a listener callback (deadlock risk)

machine.restart()       // stop() + start() in one call; optional argument passed to start()
machine.restartBlocking()

machine.destroy()       // terminal: clears all listeners, states and transitions
machine.destroyBlocking()
```

Relevant state properties:

| Property | Meaning |
|---|---|
| `isRunning` | `true` while the machine is started and not stopped |
| `isDestroyed` | `true` after `destroy()` — machine is unusable at this point |
| `hasProcessedEvents` | `true` if any event beyond `StartEvent` has been processed |

{: .note }
`stopBlocking()` must not be called from inside a listener callback when using a single-threaded
`CoroutineScope` — it will deadlock. Use `stop()` (suspendable) instead.

## Listeners

Convenience extension functions (`onTransitionTriggered {}`, `onStateEntry {}`, etc.) are available as shortcuts.
For cases where a single object should handle all machine notifications, implement `StateMachine.Listener` directly:

```kotlin
machine.addListener(object : StateMachine.Listener {
    override suspend fun onStarted(transitionParams: TransitionParams<*>) {}
    override suspend fun onTransitionTriggered(transitionParams: TransitionParams<*>) {}
    override suspend fun onTransitionComplete(activeStates: Set<IState>, transitionParams: TransitionParams<*>) {}
    override suspend fun onStateEntry(state: IState, transitionParams: TransitionParams<*>) {}
    override suspend fun onStateExit(state: IState, transitionParams: TransitionParams<*>) {}
    override suspend fun onStateFinished(state: IState, transitionParams: TransitionParams<*>) {}
    override suspend fun onStopped() {}
    override suspend fun onDestroyed() {}
})
```

All methods have default empty implementations — override only the ones you need.
`machine.removeListener(listener)` removes a previously added listener.

