---
layout: page
title: Persistence
---

# Persistence
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

- TOC
{:toc}

* **Persist** `StateMachine` - means transform it into serializable representation, such as `Serializable` object or
  JSON text, and possibly saving it into some persistent storage like file or sending by network.
* **Restoration** - is a process of restoring the `StateMachine` from the serializable representation.

There are several kinds or levels of `StateMachine` persistence (serialization). Let's look at sample use cases:

1) **Structure + configuration** - Create `StateMachine` on some process/host and send its structure and
   active configuration by network to another process/host.
   The receiver can dynamically create the same `StateMachine` instance in the same state as original one.
2) **Configuration only** - Both original and restored `StateMachine` instances are crated by identical static code
   (in a single or multiple different processes/hosts). Only active configuration can be saved and restored.

Case 1 currently lacks built-in support by the library (you can open an issue if you need something like that).
Case 2 in turn may be reached in two different ways:

1) **Persisting state** - serializing all internal data, active states, variables etc. from original `StateMachine` and
   applying them to restored one.
2) **Event recording** - serializing all incoming events, and applying them later on new `StateMachine` instance,
   which should lead it into the same state as original. This also allows to execute library callbacks (listeners)
   if necessary, which is not possible with state persistence approach.
   _Currently only this approach has built-in support._

## Event recording

The library supports event recording out of the box. To enable it you should use `EventRecordingArguments` in
`CreationArguments` when creating a machine instance by `createStateMachine()` functions family. The recording process
can be configured with `EventRecordingArguments` properties.

```kotlin
val machine = createStateMachine(
    creationArguments = buildCreationArguments { eventRecordingArguments = buildEventRecordingArguments {} }
) {
    // ...
}
```

When the machine had processed necessary events, and you want to save its state configuration, first you have to 
get the recorded events:

```kotlin
val recordedEvents = machine.eventRecorder.getRecordedEvents()
```

`RecordedEvents` object now is ready to be serialized. Currently, the library does not provide an implementation 
of serialization process, so it is up to user to write serialization code. The serialization support is planned 
using `kotlinx.serialization` library in further `KStateMachine` versions.

## Restoring StateMachine

When a user wants to restore the StateMachine, he deserializes `RecordedEvents` object and
creates StateMachine instance having exactly the same structure as original one. 
Typically, both instances are created by the same code.

Calling `restoreByRecordedEvents()` or its blocking analog `restoreByRecordedEventsBlocking()` will process
recorded events over just created StateMachine instance.

```kotlin
machine.restoreByRecordedEvents(recordedEvents)
```

`restoreByRecordedEvents()` method will start the machine if necessary.
You can configure restoration process by `restoreByRecordedEvents()` arguments.
