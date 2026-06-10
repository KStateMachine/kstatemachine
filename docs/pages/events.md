---
layout: page
title: Events
nav_order: 4
---

# Events
{: .no_toc }

## Page contents
{: .no_toc .text-delta }

- TOC
{:toc}

All events that can be processed by the library are subclassed from `Event` class.
User defined events often contain properties that serve like data inputs for your StateMachine.

Conceptually events notify a StateMachine about outside world changes.
Generally it is not recommended to confuse them with commands, see [do not section](#do-not) for details.

## Event processing

When a StateMachine is created, configured and started it is ready to process incoming events.
It is done with `processEvent()` functions family.

* `processEvent()` - suspendable version, synchronous
* `processEventBlocking()` - blocking version. Not suspendable, synchronous. Uses `kotlinx.coroutines.runBlocking`
  internally if you use StateMachine with coroutines support. Or just runs code in-place for StdLib StateMachine
  instance.
* `processEventByLaunch()` - (available in `kstatemachine-coroutines` artifact) Not suspendable, asynchronous, uses
  StateMachine's `CoroutineScope` to process event in a new coroutine by `kotlinx.coroutines.launch` function.
  Cannot be used with StdLib StateMachine instance (throws in this case).
* `processEventByAsync()` - (available in `kstatemachine-coroutines` artifact) Not suspendable, asynchronous, uses
  StateMachine's `CoroutineScope` to process event in a new coroutine by `kotlinx.coroutines.async` function.
  Returns`kotlinx.coroutines.Deferred` with `ProcessingResult`.
  Cannot be used with StdLib StateMachine instance (throws in this case).

All `processEvent` variants return or resolve to `ProcessingResult`:

| Value       | Meaning                                                                                                         |
|-------------|-----------------------------------------------------------------------------------------------------------------|
| `PROCESSED` | A matching transition was found and triggered                                                                   |
| `IGNORED`   | No matching transition found; event was passed to `IgnoredEventHandler`                                         |
| `PENDING`   | Another event is currently being processed; this event was queued or dropped depending on `PendingEventHandler` |

### Choosing a processEvent variant

| Variant                  | Suspends | Returns                      | When to use                                                                    |
|--------------------------|----------|------------------------------|--------------------------------------------------------------------------------|
| `processEvent()`         | yes      | `ProcessingResult`           | Default choice from coroutine code                                             |
| `processEventBlocking()` | no       | `ProcessingResult`           | Non-coroutine context; **never call from a listener callback** (deadlock risk) |
| `processEventByLaunch()` | no       | `Unit`                       | Fire-and-forget; you do not need the result                                    |
| `processEventByAsync()`  | no       | `Deferred<ProcessingResult>` | Non-suspending dispatch when you need to check the result later                |

`processEventByLaunch` and `processEventByAsync` are only available in the `kstatemachine-coroutines` artifact
and cannot be used with a `StdLib` machine instance.
See [Coroutines artifact](coroutines_artifact.html) for code examples.

## Ignored events

By default, state machine simply ignores events that does not match any defined transition. You can see those events if
logging is enabled or use custom `IgnoredEventHandler` for example to throw error:

```kotlin
createStateMachine(scope) {
    // ...
    ignoredEventHandler = StateMachine.IgnoredEventHandler {
        error("unexpected ${it.event}")
    }
}
```

The library provides implementation of such throwing handler by `throwingIgnoredEventHandler()` function.

## Pending events

A **pending event** is an event posted while the machine is already processing another event — the most common
case is calling `processEvent()` from inside a listener callback.

### Default behaviour — queue (deferred delivery)

`queuePendingEventHandler()` is the **default** handler. It stores incoming events in a FIFO queue and replays
them in order immediately after the current event finishes processing. This means you can freely call
`processEvent()` from any listener without worrying about re-entrancy:

```kotlin
createStateMachine(scope) {
    // queuePendingEventHandler() is already the default; shown here for clarity
    pendingEventHandler = queuePendingEventHandler()

    val state2 = state("state2")
    initialState("state1") {
        onEntry {
            // Safe: SecondEvent is queued and delivered after SwitchEvent finishes
            machine.processEvent(SecondEvent)
        }
        transitionOn<SwitchEvent> { targetState = { state2 } }
        transitionOn<SecondEvent> { targetState = { /* ... */ state2 } }
    }
}
machine.processEvent(SwitchEvent)
// Both SwitchEvent and SecondEvent are fully processed by the time this returns
```

`processEvent()` returns `ProcessingResult.PENDING` for any event that is queued rather than processed
immediately. The queued events are processed synchronously in the same call before `processEvent()` returns
to the original caller.

This is the recommended way to implement **deferred event delivery**: emit the follow-up event from an
`onEntry`, `onExit`, or transition listener callback and it will be delivered in the correct order.

### Throwing handler

Use `throwingPendingEventHandler()` during development to catch unexpected re-entrant calls:

```kotlin
createStateMachine(scope) {
    pendingEventHandler = throwingPendingEventHandler()
}
```

### Custom handler

You can provide any `PendingEventHandler` implementation to integrate with an external queue, logging
system, or dispatcher:

```kotlin
createStateMachine(scope) {
    pendingEventHandler = PendingEventHandler { eventAndArgument ->
        myExternalQueue.post(eventAndArgument.event)
    }
}
```

{: .note }
A `PendingEventHandler` that silently discards events leads to undefined machine state and missing
notifications. Prefer `queuePendingEventHandler()` unless you have a specific reason to drop events.

## Event argument

Usually if event may hold some data we define `Event` subclass, it is type safe. Sometimes if data is optional it may be
simpler to use event argument. You can specify arbitrary argument with an event in `processEvent()` function. Then you
can get this argument in a state and transition listeners.

```kotlin
val machine = createStateMachine(scope) {
    state("offState").onEntry {
        println("Event ${it.event} argument: ${it.argument}")
    }
    // ...
}
// Pass argument with event
machine.processEvent(TurnOn, 42)
```

{: .note }
Type of arguments is `Any?`, so it is not type safe to use them.

## Do not

State machine is a powerful tool to control states, so let it do its job. Do not try to rule it from outside
(selecting a target state) by sending different event types depending on business logic state. Let the state machine
to make decisions itself.

Wrong - managing target state from outside:

```kotlin
if (somethingHappend)
    machine.processEvent(GoToState1Event)
else
    machine.processEvent(GoToState2Event)
```

Correct - let the state machine to make decisions on an event:

```kotlin
machine.processEvent(SomethingHappenedEvent)
```

In certain scenarios (like a `state pattern` maybe) it is fine to use events like some kind of _setState() /
goToState()_
functions but in general it is wrong, as events are not commands.