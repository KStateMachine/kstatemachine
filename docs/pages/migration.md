---
layout: page
title: Migration guide
---

# Migration guide

{: .no_toc }

## Page contents

{: .no_toc .text-delta }

- TOC
  {:toc}

## Migrating from versions older than v0.20.0

v0.20.0 introduced built-in coroutines support and changed how state machines are created.
All public APIs gained `suspend` modifiers; blocking counterparts (with a `Blocking` suffix) were added
for non-coroutine contexts.

### If you already have or are ready to add a Kotlin Coroutines dependency

* Add both `kstatemachine` and `kstatemachine-coroutines` to your build.
* Replace machine creation with `createStateMachine` or `createStateMachineBlocking` from `kstatemachine-coroutines`,
  passing a `CoroutineScope` as the first argument.
* Prefer suspendable function variants (`start`, `stop`, `processEvent`, `undo`, etc.) whenever you are inside a
  coroutine.
* Avoid the `Blocking` suffix variants, **especially recursively** (e.g. from inside a listener callback) —
  they use `runBlocking` internally and will deadlock in that context.

See [Multithreading](multithreading.html) for the single-threaded `CoroutineScope` requirement and the
`CoroutineContext` preservation guarantee.

### If you cannot or do not want to depend on Kotlin Coroutines

* Use only the `kstatemachine` artifact.
* Replace machine creation with `createStdLibStateMachine`.
* Inside machine callbacks you can still use suspendable function variants — `suspend` is a compiler feature,
  not a Coroutines library feature, so it works without the library on the classpath.
* Outside callbacks, use the `Blocking` suffix variants; this is safe in a non-coroutine context.
* Do not call Kotlin Coroutines APIs from a machine created by `createStdLibStateMachine` — an exception will be thrown.
