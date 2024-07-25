---
layout: page
title: Multithreading and concurrency
---

# Multithreading and concurrency
{: .no_toc }

## Page contents
{: .no_toc .text-delta }

- TOC
{:toc}

KStateMachine is designed to work in single thread.
Concurrent modification of library classes will lead to race conditions.
See [kotlin coroutines](https://kstatemachine.github.io/kstatemachine/pages/multithreading.html#kotlin-coroutines) section for more info regarding coroutines environment, and how
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
may cause deadlocks if used not properly (especially recursively).
That is why you should avoid using `Blocking` APIs from coroutines and recursively (from library callbacks).

### Use single threaded `CoroutineScope`

When you create a state machine with `createStateMachine`/`createStateMachineBlocking` (with coroutines support)
functions you have to provide `CoroutineScope` on which machine will work,
this scope also contains `CoroutineContext` by coroutines design.
This is how you can control a thread where state machine works. The scope is considered to use single threaded
`CoroutineContext`.

Single thread `CoroutineScope` samples:

```kotlin
CoroutineScope(newSingleThreadContext("single thread")) // don't forget to close it
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
`kotlinx.coroutines.runBlocking` functions respectively.
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

## Migration guide from versions older than v0.20.0

### If you already have or ready to add Kotlin Coroutines dependency

* Add both `kstatemachine` and `kstatemachine-coroutines` artifacts to your build system
* Use `createStateMachine` or `createStateMachineBlocking` from `kstatemachine-coroutines` artifact to create state
  machines providing `CoroutineScope` as argument
* Use suspendable versions of functions (`start`/`stop`/`processEvent`/`undo` etc.) when possible
* Avoid using function analogs with `Blocking` suffix **(especially recursively)** as this may easily lead to deadlocks
  or race conditions depending on your use case and machine configuration

### If you can not have dependency on Kotlin Coroutines or just do not want to use it

* Use only `kstatemachine` artifact in your build system
* Use `createStdLibStateMachine` to create state machines
* Use suspendable versions of functions (`start`/`stop`/`processEvent`/`undo` etc.) when possible
  (from KStateMachine callbacks for example)
* In other cases use their analogs with `Blocking` suffix, it is ok
* If you try to use Kotlin Coroutines library from machine created by `createStdLibStateMachine` you will probably get
  an exception.
* Using suspendable code without calls to Kotlin Coroutines library is ok, as `suspend` keyword is a compiler feature,
  not library one.
