---
layout: page
title: Transitions
nav_order: 3
has_children: true
---

# Transitions
{: .no_toc }

## Page contents
{: .no_toc .text-delta }

- TOC
{:toc}

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

## Target-less transitions

Transition may have no target state (`targetState` is null) which means that state machine stays in current state when
such transition triggers, it is useful to perform some actions without changing current state:

```kotlin
greenState {
    transition<YellowEvent> {
        onTriggered { /* ... */ }
    }
}
```

{: .note }
Such transitions are also called internal or self-targeted.

## Transition type

There are two types of transitions `TransitionType.LOCAL` (default) and `TransitionType.EXTERNAL`.
Most transitions are functionally equivalent regardless of type, except when transitioning between a superstate and a
sub-state.
A local transition does not cause an exit and re-entry of the source state when the target is a sub-state of the source,
and does not cause an exit and re-entry of the target state when the target is a superstate of the source.

Use `type` argument or property of transition builder functions to set transition type:

```kotlin
transition<SwitchEvent> {
    type = EXTERNAL
    targetState = state2
}
```

## Check if the transition is triggered by StartEvent

When you start a StateMachine it enters it's initial state path. This is done with special
library defined event called `StartEvent`.

Sample: There are use cases when you need to check if the state is activated by StateMachine initialization
or due to some event processing in machine runtime (after initialization).

It can be done by checking the event type in `TransitionParams`, if it is `StartEvent` or not.
The library provides convenience `TransitionParams::isStartTransition` extension property for that purpose:

```kotlin
val machine = createStateMachine(scope) {
    val state1 = initialState("state1") {
        // will be triggered twice,
        // first time on initialization and the second after SwitchEvent processing
        onEntry {
            // true - if entering by StateMachine initialization (StartEvent)
            // false - if entering any other way, by SwitchEvent in this case.
            println(it.isStartTransition)
        }
        transitionOn<SwitchEvent> { targetState = { state2 } }
    }
    val state2 = state("state2") {
        transitionOn<SwitchEvent> { targetState = { state1 } }
    }
}
machine.processEvent(SwitchEvent)
machine.processEvent(SwitchEvent)
```

You can also access the `StartEvent` directly to read which state the machine entered:

```kotlin
onEntry {
    val startEvent = it.event as? StartEvent
    println("Initial state: ${startEvent?.startState}")
}
```

## Listen to all transitions in one place

There might be many transitions from one state to another. It is possible to listen to all of them in state machine
setup block:

```kotlin
createStateMachine(scope) {
    // ...
    onTransitionTriggered {
        // Listen to all triggered transitions here
        println(it.event)
    }
    onTransitionComplete { activeStates, transitionParams ->
        // Called after target state entry callbacks have run
        println("Active states: $activeStates")
    }
}
```

Per-transition listeners also support both callbacks via `onTriggered {}` and `onComplete {}` extension functions:

```kotlin
transition<SwitchEvent> {
    targetState = state2
    onTriggered { println("Triggered by ${it.event}") }
    onComplete { activeStates, transitionParams ->
        // Called after state2's onEntry callbacks have run
        println("Now in: $activeStates")
    }
}
```

`onTriggered` fires before target state entry callbacks; `onComplete` fires after all entry callbacks of the target
state (and its children) have completed.

## Guarded transitions

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

See [guarded transition sample](https://github.com/KStateMachine/kstatemachine/tree/master/samples/src/commonMain/kotlin/ru/nsk/samples/GuardedTransitionSample.kt)

### Guarded vs conditional transitions

|                         | `guard` on `transition()` / `transitionOn()` | `transitionConditionally()`              |
|-------------------------|----------------------------------------------|------------------------------------------|
| Target state            | Fixed at definition time                     | Chosen dynamically in `direction` lambda |
| Blocking the transition | Return `false` from `guard`                  | Return `noTransition()` from `direction` |
| Syntax                  | Shorter                                      | More flexible                            |

Use `guard` when the target state is known and you only need to decide whether to fire.
Use `transitionConditionally` when the target state itself depends on runtime data
(e.g. routing to one of several states based on a value).

```mermaid
---
title: Guarded transition diagram
---
%%{init: {'theme': 'dark'}}%%
stateDiagram-v2
State1 
[*] --> State1
State1 --> State2 : Guarded transition (if State1.value > 10)
State2 --> [*]
```

## Conditional transitions

State machine becomes more powerful tool when you can choose target state depending on your business logic (some
external data). Conditional transitions give you maximum flexibility on choosing target state and conditions when
transition is triggered.

There are following options to choose transition direction:

* `stay()` - transition is triggered but state is not changed;
* `targetState(nextState)` - transition is triggered and state machine goes to the specified state;
* `targetParallelStates(nextState1, nextState2)` transition is triggered and state machine goes to the specified
  paralleled states see [Transition targeting multiple states](#transition-targeting-multiple-states);
* `noTransition()` - transition is not triggered.

Use `transitionConditionally()` function to create conditional transition and specify a function which makes desired
decision:

```kotlin
// Suppose you have a function returning some 
// business logic value which may differ
fun getCondition() = 0

redState {
    // A conditional transition helps to control when it 
    // should be triggered and determine its target state
    transitionConditionally<GreenEvent> {
        direction = {
            when (getCondition()) {
                0 -> targetState(greenState)
                1 -> targetState(yellowState)
                2 -> targetParallelStates(parallelState1, parallelState2)
                3 -> stay()
                else -> noTransition()
            }
        }
    }
    // Same as before you can listen when conditional transition is triggered
    onTriggered { println("Conditional transition is triggered") }
}
```

## Transition targeting multiple states (fork)

`targetParallelStates()` is the programmatic equivalent of a **UML fork pseudo-state**: a single transition
splits control into several concurrent orthogonal regions, activating one target state per region simultaneously.

Use it inside `transitionConditionally()` when you want to enter a parallel state and place each of its orthogonal
regions into a specific sub-state rather than letting them start from their default initial states.
Each specified target must be a descendant (not necessarily a direct child) of a [parallel state](../states/states.md#parallel-states).

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

```mermaid
---
title: Fork transition diagram
---
%%{init: {'theme': 'dark'}}%%
stateDiagram-v2
state fork_state <<fork>>
state state2 {
  state state21 {
    state211
    state212
  }
  --
  state state22 {
    state221
    state222
  }
}
[*] --> state1
state1 --> fork_state : SwitchEvent
fork_state --> state212
fork_state --> state222
```

[PseudoState](../states/states.md#pseudo-states) targets (choice, history, etc.) are accepted and resolved
transparently at runtime, so you can pass a choice state as one of the fork targets and it will be followed to
its effective destination before activation.

## Synchronising parallel regions (join)

`joinTransition()` is the programmatic equivalent of a **UML join pseudo-state**: multiple orthogonal regions each
transition to a dedicated join-point state inside that region, and when **all** join-point states are simultaneously
active the single outgoing transition fires automatically.

Call `joinTransition()` on the **parallel state** inside its DSL block, passing one join-point state per region and
the target state to enter after joining.

```kotlin
state("parallelWork", childMode = ChildMode.PARALLEL) {

    state("download") {
        val downloadJoin = state("downloadJoin")  // no outgoing transitions → soft-blocks
        initialState("downloading") {
            transition<DownloadCompleteEvent> { targetState = downloadJoin }
        }
    }

    state("validate") {
        val validationJoin = state("validationJoin")
        initialState("validating") {
            transition<ValidationCompleteEvent> { targetState = validationJoin }
        }
    }

    joinTransition(downloadJoin, validationJoin, targetState = processing)
}
```

```mermaid
---
title: Join transition diagram
---
%%{init: {'theme': 'dark'}}%%
stateDiagram-v2
state join_state <<join>>
state parallelWork {
  state download {
    downloading --> downloadJoin : DownloadCompleteEvent
  }
  --
  state validate {
    validating --> validationJoin : ValidationCompleteEvent
  }
}
downloadJoin --> join_state
validationJoin --> join_state
join_state --> processing
[*] --> parallelWork
```

**Soft blocking**: once a region enters its join-point state, the parallel state's event-routing algorithm finds no
matching transition there and falls back to the parallel parent's own transitions — which only contain the internal
`JoinCompleteEvent`. All other events find no match and are silently ignored for that region. This is
convention-based: join-point states **must not** have outgoing user transitions.

**`FinalState` alternative**: if every region should also mark itself *finished*, use `finalState()` instead of a
plain join-point state. The parallel parent then fires `FinishedEvent` when all regions finish, giving two
notification paths for the same condition.

### Joining into a DataState

`joinTransition()` does not accept a [`DataState`](../states/states.md#data-states) as its target — there is no
event carrying the data for it. Use `joinDataTransition()` instead, which takes an additional `dataProducer` lambda
that is called once, at join time, to compute the value the target `DataState` receives on entry.

```kotlin
val result: DataState<String> = dataState("result")

state("parallelWork", childMode = ChildMode.PARALLEL) {

    state("download") {
        val downloadJoin = state("downloadJoin")
        initialState("downloading") {
            transition<DownloadCompleteEvent> { targetState = downloadJoin }
        }
    }

    state("validate") {
        val validationJoin = state("validationJoin")
        initialState("validating") {
            transition<ValidationCompleteEvent> { targetState = validationJoin }
        }
    }

    joinDataTransition(downloadJoin, validationJoin, targetState = result) {
        "download + validation complete"   // dataProducer: suspend () -> String
    }
}
```

The lambda runs inside the same coroutine that processes the join, so it may suspend (e.g. perform an async read).
Once the lambda returns, the library fires an internal `DataJoinCompleteEvent` carrying the produced value;
`result.data` is set from it exactly as if the trigger had been a regular `DataEvent`.

## Transition interruption

Typically, to calculate whether transition processing should be performed or not, you can use a guard function,
described in [Guarded transitions](#guarded-transitions). In such APIs guard function is separated from `targetState`
calculation function, sometimes it might be not convenient. So if your logic requires to mix the selection of a
`targetState` with the fact of triggering of the transition, it is more convenient to use `transitionConditionally()`
as it accepts single callback method called `direction`.

```kotlin
transitionConditionally<SwitchEvent> {
    direction = {
        if (should)
            targetState(nextState)
        else
            noTransition() // transition will not be triggered at all
    }
}
```

Both `guard` and `direction` callbacks are marked with `suspend` keyword, so you can easily call coroutines in
synchronous style inside them.

There is no way to interrupt a transition from `onTriggered()` notifications.

## Transition event type matching

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

## Finding transitions

Use `findTransition()` / `requireTransition()` to look up a transition on any state after the machine is built, for example to attach a listener dynamically.

**By name:**

```kotlin
val t = state.requireTransition("myTransition")
```

**By event type (strict):**

```kotlin
// finds only the transition whose eventClass is exactly SwitchEvent
val t = state.findTransition<SwitchEvent>()
```

**By event instance (suspend):**

```kotlin
// calls eventMatcher.match(event) — respects all custom matcher logic
val t = state.findTransition(myEvent)
```

This overload is `suspend` because `EventMatcher.match` is itself suspending. It honours the full matcher
contract, so a transition with a custom `finishedEventMatcher` or `isEqual()` will only be returned when
its matcher actually accepts the provided event instance.

## Undo transitions

Transitions may be undone with `StateMachine.undo()` function or alternatively by sending special `UndoEvent` to machine
like this `machine.processEvent(UndoEvent)`. State Machine will roll back last transition which is usually is switching
to previous state (except target-less transitions).
This API might be called as many times as needed.
To implement this feature library stores transitions in a stack, it takes memory,
so this feature is disabled by default and must be enabled explicitly using
`createStateMachine(creationArguments = buildCreationArguments { isUndoEnabled = true })` argument.
Other words this feature works like stack based FSM.

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

See [undo transition sample](https://github.com/KStateMachine/kstatemachine/tree/master/samples/src/commonMain/kotlin/ru/nsk/samples/UndoTransitionSample.kt)

## Cross-level transitions

A transition can have any state as its target. This means that the target state does not have to be on the same level in
the state hierarchy as the source state.

![Cross-level transition diagram](./../../diagrams/cross-level-transition.png)

## Transition argument

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

{: .note }
It is up to user to control that argument field is set from one listener. You can use some mutable data structure
and fill it from multiple listeners.

## Inherit transitions by grouping states

Suppose you have three states that all should have a transitions to another state. You can explicitly set this
transition for each state but with this approach complexity grows and when you add fourth state you have to remember to
add this specific transition. This problem can be solved with adding parent state which defines such transition and
groups its child states. Child states inherit there parent transitions.

```mermaid
---
title: Inherit transitions diagram
---
%%{init: {'theme': 'dark'}}%%
stateDiagram-v2
state State1 {    
    [*] --> State1_1
    State1_1 --> State1_2
    State1_2 --> State1_3
    State1_3 --> State1_1
}

[*] --> State1
State1 --> FinalState : Exit
FinalState --> [*]
```

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