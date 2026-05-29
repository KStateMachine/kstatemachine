---
layout: home
title: Overview
nav_order: 0
---

# Overview

KStateMachine is a Kotlin DSL library for creating state machines and statecharts.

The library follows concepts from the following great and well known works and specs:

* [Statecharts: A visual formalism for complex systems](https://www.wisdom.weizmann.ac.il/~dharel/SCANNED.PAPERS/Statecharts.pdf)
* [State Chart XML (SCXML)](http://www.w3.org/TR/scxml/)
* [Unified Modeling Language](https://www.omg.org/spec/UML/2.5.1/PDF)

## Workflow

Building blocks (main interfaces) of the library:

* `StateMachine` - is a collection of states and transitions between them, processes events when started
* `IState` - states where state machine can go to
* `Transition` - is an operation of moving from one state to another
* `Event` - is a base interface for events which are processed by state machine and may trigger
  transitions

Working with state machine consists of two major sequential phases:

1. Creation with initial setup and starting
2. Processing events, on which state machine can switch its states and notify about changes

```kotlin
val machine = createStateMachine(scope) {
    // Setup is made in this block ...
}
// After setup and start, it is ready to process events
machine.processEvent(FirstEvent)
// ...
machine.processEvent(OtherEvent)
```