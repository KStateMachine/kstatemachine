# KStateMachine documentation

## Table of contents

* [Table of contents](#table-of-contents)
* [Overview](#overview)
* [Workflow](#workflow)
* [Create state machine](#create-state-machine)
* [Setup states](#setup-states)
    * [Default states](#default-states)
    * [State subclasses](#state-subclasses)
    * [Listen states](#listen-states)
    * [Listen group of states](#listen-group-of-states)
    * [Payload](#payload)
* [Setup transitions](#setup-transitions)
    * [Target-less transitions](#target-less-transitions)
    * [Transition type](#transition-type)
    * [Listen to all transitions in one place](#listen-to-all-transitions-in-one-place)
    * [Guarded transitions](#guarded-transitions)
    * [Conditional transitions](#conditional-transitions)
    * [Transition targeting multiple states](#transition-targeting-multiple-states)
    * [Transition event type matching](#transition-event-type-matching)
* [Undo transitions](#undo-transitions)
* [Logging](#logging)
* [Finishing states and state machine](#finishing-states-and-state-machine)
* [Nested states](#nested-states)
    * [Inherit transitions by grouping states](#inherit-transitions-by-grouping-states)
    * [Cross-level transitions](#cross-level-transitions)
* [Composed (nested) state machines](#composed-nested-state-machines)
* [Parallel states](#parallel-states)
* [Pseudo states](#pseudo-states)
    * [Choice state](#choice-state)
    * [History state](#history-state)
* [Typesafe transitions](#typesafe-transitions)
    * [Corner cases of DataState activation](#corner-cases-of-datastate-activation)
* [Optional arguments](#optional-arguments)
    * [Event argument](#event-argument)
    * [Transition argument](#transition-argument)
* [Meta information](#meta-information)
* [Error handling](#error-handling)
    * [Ignored events](#ignored-events)
    * [Pending events](#pending-events)
    * [Exceptions from listeners](#exceptions-from-listeners)
    * [Other exceptions](#other-exceptions)
* [Multithreading and concurrency](#multithreading-and-concurrency)
* [Kotlin Coroutines](#kotlin-coroutines)
    * [Use single threaded CoroutineScope](#use-single-threaded-coroutinescope)
    * [CoroutineContext preservation guarantee](#coroutinecontext-preservation-guarantee)
    * [Additional kstatemachine-coroutines artifact](#additional-kstatemachine-coroutines-artifact)
    * [Migration guide from versions older than v0.20.0](#migration-guide-from-versions-older-than-v0200)
* [Export](#export)
    * [PlantUML](#plantuml)
    * [Mermaid](#mermaid)
    * [Controlling export output](#controlling-export-output)
* [Testing](#testing)
* [Multiplatform](#multiplatform)
* [Consider using Kotlin sealed classes](#consider-using-kotlin-sealed-classes)
    * [Object states](#object-states)
* [Do not](#do-not)
* [Known issues](#known-issues)
* [Install](#install)

## Overview

KStateMachine is a Kotlin DSL library for creating state machines and statecharts.

The library follows concepts from this two great and well known works:

* [Statecharts: A visual formalism for complex systems](https://www.wisdom.weizmann.ac.il/~dharel/SCANNED.PAPERS/Statecharts.pdf)
* [State Chart XML (SCXML)](http://www.w3.org/TR/scxml/)

## Workflow

Building blocks (main interfaces) of the library:

* `StateMachine` - is a collection of states and transitions between them, processes events when started
* `IState` - states where state machine can go to
* `Event` - is a base interface for events which are processed by state machine and may trigger
  transitions
* `Transition` - is an operation of moving from one state to another
* `TransitionParams` - information about current transition, passed to notification functions

Working with state machine consists of two major steps:

1. Creation with initial setup and starting
2. Processing events, on which state machine can switch its states and notify about changes

```kotlin
val machine = createStateMachine(scope) {
    // Setup is made in this block ...
}
// After setup and start, it is ready to process events
machine.processEvent(GreenEvent)
// ...
machine.processEvent(YellowEvent)
```

## Create state machine

First we create a state machine with one of those factory functions:

* `createStateMachine()` suspendable version (from `kstatemachine-coroutines` artifact)
* `createStateMachineBlocking()` blocking version (from `kstatemachine-coroutines` artifact)
* `createStdLibStateMachine()` - creates machine without Kotlin Coroutines support (from `kstatemachine` artifact)

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

## Setup states

### Default states

`IState` is just an interface, `DefaultState` & co. are implementations.

Use default states if you do not need to distinguish states (by type) outside from state machine. Otherwise, consider
using [state subclasses](#state-subclasses).

In state machine setup block define states with `initialState()`, `state()` and `finalState()` functions:

```kotlin
createStateMachine(scope) {
    // Use initialState() function to create initial State and add it to StateMachine
    // State machine enters this state after setup is complete
    val greenState = initialState()
    // State name is optional and is useful to getting state instance
    // after state machine setup and for debugging
    val yellowState = state("Yellow")
    val redState = finalState()
    // ...
}
```

You can use `setInitialState()` function to set initial state separately:

```kotlin
createStateMachine(scope) {
    val greenState = state()
    setInitialState(greenState)
    // ...
}
```

### State subclasses

You can use your own `IState` subclasses with `addInitialState()`, `addState()` and `addFinalState()` functions.
Subclass `DefaultState`, `DefaultFinalState` or their [data](#typesafe-transitions) analogs `DefaultDataState`
, `DefaultFinalDataState`, then you can easily distinguish your states by type when observing state changes:

```kotlin
class SomeState : DefaultState()

createStateMachine(scope) {
    val someState = addState(SomeState())
    // ...
}
```

### Listen states

In state setup blocks we can add listeners for states:

```kotlin
state {
    onEntry { println("Enter $name state") }
    onExit { println("Exit $name state") }
}
```

Or even shorter:

```kotlin
state().onEntry { /* ... */ }
```

`onEntry` and `onExit` DSL methods provide `once` argument. If it is set to `true` the listener will be removed after
the first triggering.

```kotlin
state().onEntry(once = true) { /* ... */ }
```

> [!NOTE]
> It is safe to add and remove listeners from any machine callbacks, library protects its internal loops from such
> modifications.

### Listen group of states

If you need to perform some actions depending on active statuses of two or more states use `onActiveAllOf()`
and `onActiveAnyOf()` functions.

```kotlin
onActiveAllOf(State1, State2, State3) {
    println("states active: $it")
}
```

### Payload

States often store some data.
You can define [state subclass](#state-subclasses) to add properties for your state (this is typesafe) or in some simple
cases you may use standard `payload` property of `IState` to store arbitrary data in a state.
Note that it is not typesafe (`payload` type is `Any?`), but might be handy, as you do not need to create subclasses
each time.

For use cases when you need to pass data from `Event` to `IState`, the library provides `DataState` and `DataEvent`
concept, see [typesafe transitions](#typesafe-transitions) section.

## Setup transitions

In a state setup block we define which events will trigger transitions to another states. The simplest transition is
created with `transition()` function:

```kotlin
greenState {
    // Setup transition which is triggered on YellowEvent
    transition<YellowEvent> {
        // Set target state where state machine go when this transition is triggered
        targetState = yellowState
    }
    // The same with shortcut version
    transition<RedEvent>("My transition", redState)
}
```

Same as for states we can listen to transition triggering:

```kotlin
transition<YellowEvent> {
    targetState = yellowState
    onTriggered { println("Transition to $targetState is triggered by ${it.event}") }
}
```

There is an extended version of `transition()` function, it is called `transitionOn()`. It works the same way but takes
a lambda to calculate target state. This allows to use `lateinit` state variables and to choose target state depending
on an application business logic like with [conditional transitions](#conditional-transitions) but with shorter syntax
and less flexibility:

```kotlin
createStateMachine(scope) {
    lateinit var yellowState: State

    greenState {
        transitionOn<YellowEvent> {
            targetState = { yellowState }
        }
    }

    yellowState = state {
        // ...
    }
}
```

### Target-less transitions

Transition may have no target state (`targetState` is null) which means that state machine stays in current state when
such transition triggers, it is useful to perform some actions without changing current state:

```kotlin
greenState {
    transition<YellowEvent> {
        onTriggered { /* ... */ }
    }
}
```

> [!NOTE]
> Such transitions are also called internal or self-targeted.

### Transition type

There are two types of transitions `TransitionType.LOCAL` (default) and `TransitionType.EXTERNAL`.
Most of the cases both transitions are functionally equivalent except in cases where transition
is happening between super and sub states. Local transition doesn't cause exit and entry to source state if
target state is a sub-state of a source state.
Local transition doesn't cause exit and entry to target state if target is a superstate of a source
state.

Use `type` argument or property of transition builder functions to set transition type:

```kotlin
transition<SwitchEvent> {
    type = EXTERNAL
    targetState = state2
}
```

### Listen to all transitions in one place

There might be many transitions from one state to another. It is possible to listen to all of them in state machine
setup block:

```kotlin
createStateMachine(scope) {
    // ...
    onTransitionTriggered {
        // Listen to all triggered transitions here
        println(it.event)
    }
}
```

### Guarded transitions

Guarded transition is triggered only if specified guard function returns `true`. Guarded transition is a special kind
of [conditional transition](#conditional-transitions) with shorter syntax. Use `transition()` or `transitionOn()`
functions to create guarded transition:

```kotlin
state1 {
    transition<SwitchEvent> {
        guard = { value > 10 }
        targetState = state2
        // ...
    }
}
```

See [guarded transition sample](https://github.com/nsk90/kstatemachine/tree/master/samples/src/commonMain/kotlin/ru/nsk/samples/GuardedTransitionSample.kt)

![Guarded transition diagram](./diagrams/guarded-transition.png)

### Conditional transitions

State machine becomes more powerful tool when you can choose target state depending on your business logic (some
external data). Conditional transitions give you maximum flexibility on choosing target state and conditions when
transition is triggered.

There are three options to choose transition direction:

* `stay()` - transition is triggered but state is not changed;
* `targetState(nextState)` - transition is triggered and state machine goes to the specified state;
* `noTransition()` - transition is not triggered.

Use `transitionConditionally()` function to create conditional transition and specify a function which makes desired
decision:

```kotlin
redState {
    // A conditional transition helps to control when it 
    // should be triggered and determine its target state
    transitionConditionally<GreenEvent> {
        direction = {
            // Suppose you have a function returning some 
            // business logic value which may differ
            fun getCondition() = 0

            when (getCondition()) {
                0 -> targetState(greenState)
                1 -> targetState(yellowState)
                2 -> stay()
                else -> noTransition()
            }
        }
    }
    // Same as before you can listen when conditional transition is triggered
    onTriggered { println("Conditional transition is triggered") }
}
```

### Transition targeting multiple states

When you work with parallel states, you may want to specify multiple states as a transition target, specifying
a target state for each parallel state region.

This may be done with `targetParallelStates()` method inside `transitionConditionally()` transition builder function.
Each specified state must be a child (not necessary direct) of a parallel state.

```kotlin
initialState("state1") {
    transitionConditionally<SwitchEvent> {
        direction = { targetParallelStates(state212, state222) }
    }
}
state("state2", childMode = ChildMode.PARALLEL) {
    state("state21") {
        initialState("state211")
        state212 = state("state212")
    }
    state("state22") {
        initialState("state221")
        state222 = state("state222")
    }
}
```

### Transition event type matching

By default, event type that triggers transition is matched as instance of specified event class. For
example `transition<SwitchEvent>()` matches `SwitchEvent` class and its subclasses. If you have event hierarchy it might
be necessary to control matching mechanism, it might be done with `eventMatcher` argument of transition builder
functions:

```kotlin
transition<SwitchEvent> {
    eventMatcher = isEqual()
}
```

There are two predefined event matchers:

* `isInstanceOf()` matches specified class and its subclasses (default)
* `isEqual()` matches only specified class

You can define your own matchers by subclassing `EventMatcher` class.

## Undo transitions

Transitions may be undone with `StateMachine.undo()` function or alternatively by sending special `UndoEvent` to machine
like this `machine.processEvent(UndoEvent)`. State Machine will roll back last transition which is usually is switching
to previous state (except target-less transitions).
This API might be called as many times as needed.
To implement this feature library stores transitions in a stack, it takes memory,
so this feature is disabled by default and must be enabled explicitly using 
`createStateMachine(creationArguments = CreationArguments(isUndoEnabled = true))` argument.

Undo functionality is implemented as `Event`, so it possible to call `undo()` from notification callbacks, if you use
`QueuePendingEventHandler` (which is default) or its analog.

For example if states of state machine represent UI screens, `undo()` acts like some kind of `navigateUp()` function.

Internally every `UndoEvent` is transformed to `WrappedEvent` which stores original event and argument.
When some state is entered as a result of undo operation you can access original event and argument with
`unwrappedEvent` and `unwrappedArgument` extension properties of `TransitionParams` class.
Original event is the event that triggered original transition to this state.

```kotlin
state {
    onEntry { transitionParams -> // when called as result of undo() operation
        transitionParams.event // is WrappedEvent
        transitionParams.unwrappedEvent // is original event
        (transitionParams.event as WrappedEvent).event // same as using unwrappedEvent extension
    }
}
```

See [undo transition sample](https://github.com/nsk90/kstatemachine/tree/master/samples/src/commonMain/kotlin/ru/nsk/samples/UndoTransitionSample.kt)

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

## Finishing states and state machine

Some of state machines and states are infinite, but other ones may finish.

* In `ChildMode.EXCLUSIVE` state or state machine finishes when enters top-level final state.
* In `ChildMode.PARALLEL` state or state machine finishes when all its direct children has finished.

To make a state final, it must implement `FinalState` marker interface.
Built-in implementation of such state is `DefaultFinalState`.
It can be created directly with `finalState()` function or be subclassed and added with `addFinalState()` function.
Alternatively you can inherit basic `DefaultState` and mark it with `FinalState` explicitly like here:

```kotlin
sealed class States : DefaultState() {
    object State1 : States()
    object State2 : States(), FinalState
}
```

See [Finished state sample](https://github.com/nsk90/kstatemachine/tree/master/samples/src/commonMain/kotlin/ru/nsk/samples/FinishedStateSample.kt)

Finishing of states and state machines is treated little differently.
State machine that was finished stops processing incoming events.
But when some nested state is finished its transitions are still active,
only notification is triggered and `isFinished` property set.

Notifications about finishing are available in two forms:

1. Triggering of `onFinished()` listener callback. This is the only option for `StateMachine`.

    ```kotlin
    val machine = createStateMachine(scope) {
        initialFinalState("final")
    
        onFinished { println("State machine is finished") }
    }
    machine.isFinished // is true
    ```

2. Generation and processing of special `FinishedEvent`. This option is also available for composite states and useful
   for performing transitions on finishing:

    ```kotlin
    createStateMachine(scope) {
        val state2 = state("state2")
   
        initialState("state1") {
            initialFinalState("final")
            
            // this transition matches only FinishedEvent generated by finishing of "state1"
            transition<FinishedEvent>(targetState = state2)
        }
    }
    ```

   Transition for `FinishedEvent` is detected by the library and matched by special kind of `EventMatcher`,
   so such transition is triggered only for `FinishedEvent` that corresponds to this state.
   `FinishingEvent` generated by finishing of another state will not trigger such transition.

   If `FinalState` that triggered `FinishedEvent` is also a `DataState` then its `data` field will be copied
   into `FinishedEvent`.
   See [transition on FinishedEvent sample](https://github.com/nsk90/kstatemachine/tree/master/samples/src/commonMain/kotlin/ru/nsk/samples/FinishedEventSample.kt)

## Nested states

With nested states you can build hierarchical state machines and inherit transitions by grouping states.

To create nested states simply use same functions (`state()`, `initialState()` etc.) as for state machine but in state
setup block:

```kotlin
val machine = createStateMachine(scope) {
    val topLevelState = initialState {
        // ...
        val nestedState = initialState {
            // ...
            initialState()
            state()
            finalState()
        }
    }
}
```

### Inherit transitions by grouping states

Suppose you have three states that all should have a transitions to another state. You can explicitly set this
transition for each state but with this approach complexity grows and when you add fourth state you have to remember to
add this specific transition. This problem can be solved with adding parent state which defines such transition and
groups its child states. Child states inherit there parent transitions.

![Inherit transitions diagram](./diagrams/inherit-transitions.png)

A child state can override an inherited transition. To override parent transition child state should define any
transition that matches the event.

```kotlin
createStateMachine(scope) {
    val state2 = state("state2")
    // all nested states inherit this parent transition
    transition<SwitchEvent> { targetState = state2 }

    // child state overrides transitions for all events
    initialState("state1") { transition<Event>() }
}
```

### Cross-level transitions

A transition can have any state as its target. This means that the target state does not have to be on the same level in
the state hierarchy as the source state.

![Cross-level transition diagram](./diagrams/cross-level-transition.png)

## Composed (nested) state machines

`StateMachine` is a subclass of `IState`, this allows to use it as a child of another state machine like a simple state.
The parent state machine treats the child machine as an atomic state. It is not possible to reference states of a child
machine from parent transitions and vise versa. Child machine is automatically started when parent enters it. Events
from parent machine are not passed to it child machines. Child machine receives events only from its
own `processEvent()`
calls.

## Parallel states

Sometimes it might be useful to have a state machine containing mutually exclusive properties. Assume your laptop, it
might be charging, sleeping, its lid may be open at the same time. If you try to create a state machine for those
properties you will have _3 * 3 = 9_ amount of states
(_"Charging, Sleeping, LidOpen"_, _"OnBattery, Sleeping, LidOpen"_, etc...). This is where parallel states come into
play. This feature helps to avoid combinatorial explosion of states. Using parallel states this machine will look like
this:

![Parallel states diagram](./diagrams/parallel-states.png)

Set `childMode` argument of a state machine, or a state creation functions to `ChildMode.PARALLEL`. When a parent state
with parallel child mode is entered or exited, all its child states will be simultaneously entered or exited:

```kotlin
createStateMachine(scope, childMode = ChildMode.PARALLEL) {
    state("Charger") {
        initialState("Charging") { /* ... */ }
        state("OnBattery") { /* ... */ }
    }
    state("Lid") { /* ... */ }
    // ..
}
```

Currently, there is no way to process multiple transitions for one event by using parallel states, only one transition
may be triggered for each event. Such behaviour might be easily emulated using separated events for each
parallel branch (region).

## Pseudo states

Pseudo states are special kind of states that machine cannot enter, but they are useful to describe additional
logic in machine behaviour.

### Choice state

Choice state allows to select target state depending on some condition. When transition targeting a choice state is
triggered, choice function is evaluated and machine goes to resulting state:

```kotlin
choiceState {
    if (event.value > 3) State1 else State2
}
```

There is also `choiceDataState()` function available for choosing between `DataState`s. You can define `dataTransition`
to target such pseudo data state.

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

## Typesafe transitions

It is a common case when a state expects to receive some data from an event. Library provides typesafe API for such
case. It is implemented with `DataEvent` and `DataState`. Both interfaces are parameterized with data type. To create
typesafe transition use `dataTransition()` and `dataTransitionOn()` functions. This API helps to ensure that event data
parameter type matches data parameter type that is expected by a target state of a transition. Compiler will protect you
from defining a transition with incompatible data type parameters of event and target state.

```kotlin
class StringEvent(override val data: String) : DataEvent<String>

createStateMachine(scope) {
    val state2 = dataState<String> {
        onEntry { println("State data: $data") }
    }

    initialState {
        dataTransition<StringEvent, String> { targetState = state2 }
    }
}
```

`DataState`'s `data` field is set and might be accessed only while the state is active. At the moment when `DataState`
is activated it requires data value from a `DataEvent`. You can use `lastData` field to access last data value even
after state exit, it falls back to `defaultData` if provided or throws.

### Target-less data transitions

You can define target-less transitions for `DataState`. Please, note that if you want such transition to change state's
`data` field, it should be `EXTERNAL` type. If target-less transition is `LOCAL` it does not change states data.
This is related to the way how `DataState` is implemented, `data` field is changed only on state entry moment.

### Corner cases of `DataState` activation

1. Implicit activation. `DataState` might be activated by `Event` (not `DataEvent`) that is targeting its child state.
   In this case `data` field of `DataState` is assigned with `lastData` field value.
   If state is activating the first time `lastData` falls back to `defaultData` if provided, otherwise exception is
   thrown.
2. Activation by `undo()` of `UndoEvent`. This works same way as undone transition.
3. Activation by `FinishedEvent`. `FinishedEvent` may contain non-null data field. `DataState` receives this data
   if its type matches. `DataExtractor` class is responsible for matching. Such transition might be created only by
   `transitionConditionally()` function.
4. Activation by non data event. This should not be necessary, but it might be done manually, same way as in case 3.
   Using custom `DataExtractor` you can pass any data from any event type to `DataState`.

## Optional arguments

> [!NOTE]
> Type of arguments is `Any?`, so it is not type safe ot use them.

### Event argument

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

### Transition argument

If transition listener produce some data, you can pass it to target state as a transition argument:

```kotlin
val second = state("second").onEntry {
    println("Transition argument: ${it.transition.argument}")
}
state("first") {
    transition<SwitchEvent> {
        targetState = second
        onTriggered { it.transition.argument = 42 }
    }
}
```

> [!NOTE]
> It is up to user to control that argument field is set from one listener. You can use some mutable data structure
> and fill it from multiple listeners.

## Meta information

The library provides `metaInfo` property for `IState` and `Transition` types.
`MetaInfo` is a marker interface allowing to attach some static information to library primitives.
This mechanism is extendable and users may add their own `MetaInfo` sub interfaces/classes if necessary.
Currently the only standard implementation is `UmlMetaInfo` which is useful for export feature.
See [controlling export output](#controlling-export-output).

> [!NOTE]
> MetaInfo considered to be immutable data by design

## Error handling

### Ignored events

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

### Pending events

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

> [!NOTE]
> `PendingEventHandler` that does nothing will not let you process pending events (they will be dropped) as it
> leads to undefined machine state and mixed notifications.

### Exceptions from listeners

Event though `KStateMachine` assumes that listener callbacks should not throw exceptions, it may happen in practice.
If your app code throws exceptions in a listener callbacks library catches them, completes transition successfully and
passes the first occurred exception to `listenerExceptionHandler`. It simply rethrows exception by default, but you may
want to mute them with custom handler for example.

### Other exceptions

Exceptions coming from other client code callbacks, that are considered to be no-throwing (like guard functions of
transitions) are not caught. Machine will be automatically destroyed with `destroy()` function on such exceptions,
as it is in unpredictable state and cannot be used anymore.
Calling `processEvent()` on destroyed machine will throw also.

## Multithreading and concurrency

KStateMachine is designed to work in single thread.
Concurrent modification of library classes will lead to race conditions.
See [kotlin coroutines](#kotlin-coroutines) section for more info regarding coroutines environment, and how
the library helps you to support this requirement.

## Kotlin Coroutines

Starting from `KStateMachine v0.20.0` the library has built-in coroutines support.
All its callbacks and other APIs were marked with `suspend` modifier, allowing to use coroutines from them.
You can still use all KStateMachine features without Kotlin Coroutines library dependency as `suspend` keyword
is implemented at compiler level and Coroutines library is not really necessary to start coroutines.

Many functions like `createStateMachine`/`start`/`stop`/`processEvent`/`undo` etc. are suspendable, but all of them
has analogs with `Blocking` suffix which are not marked with `suspend` keyword.
If you use KStateMachine with coroutines support you should prefer suspendable function versions.
Note that `Blocking` versions internally use `kotlinx.coroutines.runBlocking` function which is rather dangerous and
may cause deadlocks if used not properly. That is why you should avoid using `Blocking` APIs from coroutines and
recursively (from library callbacks).

### Use single threaded `CoroutineScope`

When you create a state machine with `createStateMachine`/`createStateMachineBlocking` (with coroutines support)
functions you have to provide `CoroutineScope` on which machine will work,
this scope also contains `CoroutineContext` by coroutines design.
This is how you can control a thread where state machine works. The scope is considered to use single threaded
`CoroutineContext`.

Single thread `CoroutineScope` samples:

```kotlin
CoroutineScope(newSingleThreadContext("single thread"))
CoroutineScope(Dispatchers.Main)
```

Using multithreaded `CoroutineContext` like `Dispatchers.Default` or `Dispatchers.IO` will lead to race
conditions, it is not correct.

Even `Dispatchers.Default.limitedParallelism(1)` that seems to be ok at glance,
does not provide guarantee that each coroutine will be executed on the same single thread, it only limits the amount of
used threads. So race condition still takes place, as nothing forces threads, running on different processor cores,
to update variable values in their processor core caches, so outdated values could be used from core cache. Other words, 
one thread does not to know about variable changes made by other one. This known as __visibility guarantee__,
that `volatile` keyword provides on `jvm`.

### `CoroutineContext` preservation guarantee

Suspendable functions and their `Blocking` analogs internally switch current execution `СoroutineСontext`
(from which they are called) to state machines one, using `kotlinx.coroutines.withContext` or
`kotlinx.coroutines.runBlocking` arguments respectively.
This is `CoroutineContext` preservation guarantee that the library provides.
Note that if you created machine with the scope containing `kotlinx.coroutines.EmptyCoroutineContext` switching will not
be performed. So if the StateMachine is created with correct (meeting above conditions) scope it is safe to call
suspendable methods like `processEvent()` from any context/thread due to internal context preservation.
StateMachine that was created by `createStdLibStateMachine()` (without coroutines support) does not perform any context
switching and of course does NOT provide any `CoroutineContext` preservation guarantee.

Multithreading is always complicated and hard to explain, so you can also check this sample
regarding working with state machine from coroutines running from multiple threads:

```kotlin
// runBlocking starts an infinite event loop on current running thread,
// so it produces correct single threaded CoroutineContext for a StateMachine.
runBlocking { // defines non-empty coroutine context for state machine
    val machineThread = Thread.currentThread()
    val machineScope = this

    val machine = createStateMachine(machineScope) {
        onStarted { check(Thread.currentThread() == machineThread) }

        val state2 = state("state2")
        initialState("state1") {
            transition<SwitchEvent> {
                targetState = state2
                onTriggered { check(Thread.currentThread() == machineThread) }
            }
        }
    }

    withContext(Dispatchers.Default) {
        check(Thread.currentThread() != machineThread) // suppose we are working from some other thread

        // OK, will be processed on state machine context as `processEvent` is suspendable and switches context
        // internally and context is not EmptyCoroutineContext
        machine.processEvent(SwitchEvent)

        // But this is NOT OK, this will be a race condition as this property is muted from state machines thread
        // if (machine.isRunning) { /* do something */ }

        withContext(machineScope.coroutineContext) {
            // OK again as we switched context explicitly before accessing property
            if (machine.isRunning) { /* do something */
            }
            check(Thread.currentThread() == machineThread)
        }
    }
}
```

### Additional kstatemachine-coroutines artifact

Contains additional functions to work with KStateMachine depending on Kotlin Coroutines library

* `createStateMachine()` / `createStateMachineBlocking()` creates state machine with specified `CoroutineScope`
* `stateMachineNotificationFlow()` returns a `SharedFlow` of all machine notifications
* `activeStatesFlow()` returns a `StateFlow` of active machine states

### Migration guide from versions older than v0.20.0

#### If you already have or ready to add Kotlin Coroutines dependency

* Add both `kstatemachine` and `kstatemachine-coroutines` artifacts to your build system
* Use `createStateMachine` or `createStateMachineBlocking` from `kstatemachine-coroutines` artifact to create state
  machines providing `CoroutineScope` as argument
* Use suspendable versions of functions (`start`/`stop`/`processEvent`/`undo` etc.) when possible
* Avoid using function analogs with `Blocking` suffix **(especially recursively)** as this may easily lead to deadlocks
  or race conditions depending on your use case and machine configuration

#### If you can not have dependency on Kotlin Coroutines or just do not want to use it

* Use only `kstatemachine` artifact in your build system
* Use `createStdLibStateMachine` to create state machines
* Use suspendable versions of functions (`start`/`stop`/`processEvent`/`undo` etc.) when possible
  (from KStateMachine callbacks for example)
* In other cases use their analogs with `Blocking` suffix, it is ok
* If you try to use Kotlin Coroutines library from machine created by `createStdLibStateMachine` you will probably get
  an exception.
* Using suspendable code without calls to Kotlin Coroutines library is ok, as `suspend` keyword is a compiler feature,
  not library one.

## Export

The library supports export into PlantUML and Mermaid diagram drawing systems. They both use PlantUML text format.
Mermaid supports fewer features then PlantUML itself.
Please note that both of them have their own limitations and corner cases.

> [!NOTE]
> Transitions that use lambdas like `transitionConditionally()` and `transitionOn()` or `choiceState()` etc.,
> are not exported by default.
> You can enable their export with `unsafeCallConditionalLambdas` flag of `exportToPlantUml()`/`exportToMermaid()`
> functions.
> With `unsafeCallConditionalLambdas` flag set, user defined lambdas that are passed to the library to calculate next
> state would be called during export process. This will give more complete (still not full) export output,
> but may cause runtime errors depending on what the lambda actually do. As it may touch application data that is not
> valid when export is running, also `event` argument will be faked by unsafe cast, so touching it
> will cause `ClassCastException`
> That is why `unsafeCallConditionalLambdas` flag should be considered as debug/development tool only.

### PlantUML

Use `exportToPlantUml()`/`exportToPlantUmlBlocking()` extension function to export state machine
to [PlantUML state diagram](https://plantuml.com/en/state-diagram).
`showEventLabels` flag allows to include `Event` types into the output.

```kotlin
val machine = createStateMachine(scope) { /* ... */ }
println(machine.exportToPlantUml())
```

Copy/paste resulting output to [Plant UML online editor](http://www.plantuml.com/plantuml/)

See [PlantUML nested states export sample](https://github.com/nsk90/kstatemachine/tree/master/samples/src/commonMain/kotlin/ru/nsk/samples/PlantUmlExportSample.kt)

### Mermaid

`Mermaid` uses almost the same text format as `PlantUML` for compatibility reasons.

Use `exportToMermaid()`/`exportToToMermaidBlocking()` extension function to export state machine
to [Mermaid state diagram](https://mermaid.js.org/syntax/stateDiagram.html).
`showEventLabels` flag allows to include `Event` types into the output.

```kotlin
val machine = createStateMachine(scope) { /* ... */ }
println(machine.exportToMermaid())
```

* `Intellij IDEA` users may use official [Mermaid plugin](https://plugins.jetbrains.com/plugin/20146-mermaid)
  to view diagrams directly in IDE for file types: `.mmd` and `.mermaid`.
* or copy/paste resulting output to [Mermaid live editor](https://mermaid.live/)

See [Mermaid nested states export sample](https://github.com/nsk90/kstatemachine/tree/master/samples/src/commonMain/kotlin/ru/nsk/samples/MermaidExportSample.kt)

### Controlling export output

To beautify and enrich export output, you can use `UmlMetaInfo` for both `IState` and `Transition`:

```kotlin
state("State1") {
    metaInfo = UmlMetaInfo(
        umlLabel = "State 1 long label",
        umlStateDescriptions = listOf("Description 1", "Description 2"),
        umlNotes = listOf("Note 1", "Note 2"),
    )
}
```

See [PlantUML with MetaInfo export sample](https://github.com/nsk90/kstatemachine/tree/master/samples/src/commonMain/kotlin/ru/nsk/samples/PlantUmlExportWithMetaInfoSample.kt)

## Testing

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

## Multiplatform

Starting from v0.22.0 KStateMachine has moved to Kotlin Multiplatform only with `JVM` platform support.
In v0.22.1 `iOS` support has been added also.

_If you need missing platform support please create a GitHub issue._

## Consider using Kotlin `sealed` classes

With sealed classes for states and events your state machine structure may look simpler. Try to compare this two samples
they both are doing the same thing but using of sealed classes makes code self explaining:

[Minimal sealed classes sample](https://github.com/nsk90/kstatemachine/tree/master/samples/src/commonMain/kotlin/ru/nsk/samples/MinimalSealedClassesSample.kt)
vs
[Minimal syntax sample](https://github.com/nsk90/kstatemachine/tree/master/samples/src/commonMain/kotlin/ru/nsk/samples/MinimalSyntaxSample.kt)

Also sealed classes eliminate need of using `lateinit` states variables or reordering of states in state machine setup
block to have a valid state references for transitions.

### Object states

Keep in mind that states are mutated by machine instance, defining them with `object` keyword (i.e. singleton) often
makes your states live longer than machine. It is common use case when you have multiple similar machines
that are using same singleton states sequentially. Library detects such cases automatically by default
(see `autoDestroyOnStatesReuse` argument of `CreationArguments` structure) and cleans states allowing for future reuse.
You can disable automatic machine destruction on state reuse, and call `StateMachine.destroy()` manually if required,
or just do not use `object` keyword for defining states.
If you have your own `DefaultState` subclasses that are singletons and has data fields, use
`onCleanup()` callback to clean your data before state reuse.

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

## Known issues

It is not recommended to use generic classes as events and as argument of `DataState`. JVM removes
difference between generic classes with different argument types, this is known as type erasure.
So library cannot separate such types from each other at runtime. When it is necessary to check that some object is an
instance of
a class, such check may be positive for class parameterized with any type.
So it's easier avoid using generic types in such cases. You have to use custom `EventMatcher`s and `DataExtractor`'s
that
will use some additional information to compare such types, or be sure that such invalid comparison never happens.

## Install

KStateMachine is available on `Maven Central` and `JitPack` repositories.

The library consists of 2 components:

* `kstatemachine` - (mandatory) state machine implementation (depends only on Kotlin Standard library)
* `kstatemachine-coroutines` - (optional) add-ons for working with coroutines (depends on Kotlin Coroutines library)

Please note that starting from `v0.22.0` the library switched to Kotlin Multiplatform and artifact naming has changed.

### Maven Central

Add dependencies:

```kotlin
// kotlin
dependencies {
    // multiplatform artifacts (starting from 0.22.0)
    implementation("io.github.nsk90:kstatemachine:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines:<Tag>")
    // or JVM/Android artifacts (starting from 0.22.0)
    implementation("io.github.nsk90:kstatemachine-jvm:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-jvm:<Tag>")
    // or iOS artifacts (starting from 0.22.1)
    implementation("io.github.nsk90:kstatemachine-iosarm64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-iosarm64:<Tag>")

    implementation("io.github.nsk90:kstatemachine-iosx64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-iosx64:<Tag>")

    implementation("io.github.nsk90:kstatemachine-iossimulatorarm64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-iossimulatorarm64:<Tag>")
}
```

```groovy
// groovy
dependencies {
    // multiplatform artifacts
    implementation 'io.github.nsk90:kstatemachine:<Tag>'
    implementation 'io.github.nsk90:kstatemachine-coroutines:<Tag>' // optional
    // etc..
}
```

Where `<Tag>` is a library version.

You can see official docs
about [dependencies on multiplatform libraries](https://kotlinlang.org/docs/multiplatform-add-dependencies.html#library-used-in-specific-source-sets)

### JitPack (deprecated)

Currently, `JitPack` does not support Kotlin multiplatform artifacts.
So versions starting from `0.22.0` are not available there, use `Maven Central` instead.

Add the [JitPack](https://jitpack.io/#nsk90/kstatemachine/Tag) repository to your build file. Add it in your
root `build.gradle` at the end of repositories:

```kotlin
// kotlin
repositories {
    //  ...
    maven { url = uri("https://jitpack.io") }
}
```

```groovy
// groovy
allprojects {
    repositories {
        //  ...
        maven { url 'https://jitpack.io' }
    }
}
```

Add dependencies:

```kotlin
// kotlin
dependencies {
    implementation("com.github.nsk90:kstatemachine:<Tag>")
    // note that group is different in second artifact, long group name also works for first artifact but not vise versa
    // it is some strange JitPack behaviour
    implementation("com.github.nsk90.kstatemachine:kstatemachine-coroutines:<Tag>") // optional
}
```

```groovy
// groovy
dependencies {
    implementation 'com.github.nsk90:kstatemachine:<Tag>'
    // note that group is different in second artifact, long group name also works for first artifact but not vise versa
    // it is some strange JitPack behaviour
    implementation 'com.github.nsk90.kstatemachine:kstatemachine-coroutines:<Tag>' // optional
}
```

Where `<Tag>` is a library version.