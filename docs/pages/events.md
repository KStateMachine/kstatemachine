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

Conceptually events notify a StateMachine about out world changes.
Generally it is not recommended to confuse them with commands, see [do not section](#do-not) for details.

## Event processing

When a StateMachine is created, configured and started it is ready to process incoming events.
It is done with `processEvent()` functions family.

* `processEvent()` - suspendable version
* `processEventBlocking()` - blocking version. Not suspendable, uses `kotlinx.coroutines.runBlocking`
  internally if you use StateMachine with coroutines support. Or just runs code in-place for StdLib StateMachine
  instance.
* `processEventByLaunch()` - (available in `kstatemachine-coroutines` artifact) Not suspendable, uses StateMachine's
  `CouroutineScope` to process event in a new coroutine by `kotlinx.coroutines.launch` function.
  Cannot be used with StdLib StateMachine instance (throws in this case).
* `processEventByAsync()` - (available in `kstatemachine-coroutines` artifact) Not suspendable, uses StateMachine's
  `CouroutineScope` to process event in a new coroutine by `kotlinx.coroutines.async` function.
  Returns`kotlinx.coroutines.Deffered` with `ProcessingResult`.
  Cannot be used with StdLib StateMachine instance (throws in this case).

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

Pending events are such events that are posted for processing while another event is already processing, for example
from listeners callbacks.

Default `PendingEventHandler` that is created with `queuePendingEventHandler()` stores such events in queue and machine
processes them all after a current one. This allows to call `processEvent()` from listeners callbacks.

If you are using another `PendingEventHandler` implementation, like logging or throwing one created by
`throwingPendingEventHandler()` function, then behaviour of calling `processEvent()` while state machine is already
processing event will depend on `PendingEventHandler` implementation. Pending event may be simply dropped or exception
thrown. Alternatively with custom `PendingEventHandler` you can post such events to some queue to process them later
passing to `processEvent()`. Using of throwing `PendingEventHandler` sample:

```kotlin
createStateMachine(scope) {
    // ...
    pendingEventHandler = throwingPendingEventHandler()
}
```

{: .note }
`PendingEventHandler` that does nothing will not let you process pending events (they will be dropped) as it
leads to undefined machine state and mixed notifications.

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
Type of arguments is `Any?`, so it is not type safe ot use them.

## Do not

State machine is a powerful tool to control states, so let it do its job. Do not try to rule it from outside
(selecting a target state) by sending different event types depending on business logic state. Let the state machine
to make decisions itself.

Wrong - managing target state from outside:

```kotin
if (somethingHappend)
    machine.processEvent(GoToState1Event)
else 
    machine.processEvent(GoToState2Event)
```

Correct - let the state machine to make decisions on an event:

```kotin
machine.processEvent(SomethingHappenedEvent)
```

In certain scenarios (like a `state pattern` maybe) it is fine to use events like some kind of _setState() /
goToState()_
functions but in general it is wrong, as events are not commands.