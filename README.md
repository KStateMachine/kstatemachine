![KStateMachine](./docs/kstatemachine-logo.png)

![Build and test with Gradle](https://github.com/nsk90/kstatemachine/workflows/Build%20and%20test%20with%20Gradle/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=nsk90_kstatemachine&metric=alert_status)](https://sonarcloud.io/dashboard?id=nsk90_kstatemachine)
![Maven Central](https://img.shields.io/maven-central/v/io.github.nsk90/kstatemachine)
[![](https://jitpack.io/v/nsk90/kstatemachine.svg)](https://jitpack.io/#nsk90/kstatemachine)
[![codecov](https://codecov.io/gh/nsk90/kstatemachine/branch/master/graph/badge.svg?token=IR2JR43FOZ)](https://codecov.io/gh/nsk90/kstatemachine)
[![Android Arsenal]( https://img.shields.io/badge/Android%20Arsenal-KStateMachine-green.svg?style=flat )]( https://android-arsenal.com/details/1/8276 )
![multiplatform support](https://img.shields.io/badge/multiplatform-jvm-brightgreen)

KStateMachine is a Kotlin DSL library for creating [state machines](https://en.wikipedia.org/wiki/Finite-state_machine)
and hierarchical state machines ([statecharts](https://www.sciencedirect.com/science/article/pii/0167642387900359/pdf)).

## Overview

The library follows concepts from this two great and well known works:

* [Statecharts: A visual formalism for complex systems](https://www.wisdom.weizmann.ac.il/~dharel/SCANNED.PAPERS/Statecharts.pdf)
* [State Chart XML (SCXML)](http://www.w3.org/TR/scxml/)

Integration features are:

* Kotlin DSL syntax for defining state machine structure. Using without DSL is also possible
* Zero dependency. It is written in pure Kotlin, it does not depend on any third party libraries or Android SDK
* [Built-in support for Kotlin Coroutines.](https://nsk90.github.io/kstatemachine/#kotlin-coroutines)
  But you can fully use KStateMachine without Kotlin Coroutines dependency if necessary.
* Kotlin [Multiplatform](https://nsk90.github.io/kstatemachine/#multiplatform) support
* [Backward compatible](https://github.com/nsk90/kstatemachine/blob/master/buildSrc/src/main/kotlin/ru/nsk/Versions.kt)
  till Kotlin 1.5

State management features:

* Event based - [transitions](https://nsk90.github.io/kstatemachine/#setup-transitions) are performed by processing
  incoming events
* [Listeners](https://nsk90.github.io/kstatemachine/#listen-states) for machine, states,
  [state groups](https://nsk90.github.io/kstatemachine/#listen-group-of-states) and transitions. Listener callbacks are
  shipped with information about
  current transition
* [Guarded](https://nsk90.github.io/kstatemachine/#guarded-transitions)
  and [Conditional transitions](https://nsk90.github.io/kstatemachine/#conditional-transitions) with dynamic target
  state which is calculated in a moment of event processing depending on application business logic
* [Nested states](https://nsk90.github.io/kstatemachine/#nested-states) - hierarchical state machines (HSMs)
  with [cross-level transitions](https://nsk90.github.io/kstatemachine/#cross-level-transitions) support
* [Composed (nested) state machines.](https://nsk90.github.io/kstatemachine/#composed-(nested)-state-machines) Use
  state machines as atomic child states
* [Pseudo states](https://nsk90.github.io/kstatemachine/#pseudo-states) for additional logic in machine behaviour
* [Typesafe transitions](https://nsk90.github.io/kstatemachine/#typesafe-transitions) to pass data in typesafe way
  from event to state
* [Parallel states](https://nsk90.github.io/kstatemachine/#parallel-states) to avoid a combinatorial explosion of
  states
* [Undo transitions](https://nsk90.github.io/kstatemachine/#undo-transitions) for navigating back to previous state
* [Argument](https://nsk90.github.io/kstatemachine/#arguments) passing for events and transitions
* [Pending events](https://nsk90.github.io/kstatemachine/#pending-events) support
* [Export state machine](https://nsk90.github.io/kstatemachine/#export) structure
  to [PlantUML](https://plantuml.com/);
* Built-in [logging](https://nsk90.github.io/kstatemachine/#logging) support
* [Testable](https://nsk90.github.io/kstatemachine/#testing) - you can run state machine from specified state
* Well tested. All features are covered by tests

_The library is still in a development phase. You are welcome to propose useful features._

_Don't forget to push the ⭐ if you like this project._

## SEE FULL [DOCUMENTATION HERE](https://nsk90.github.io/kstatemachine)

## Quick start sample (finishing traffic light)

![Traffic light diagram](./docs/diagrams/finishing-traffic-light.svg)

```kotlin

object SwitchEvent : Event

sealed class States : DefaultState() {
    object GreenState : States()
    object YellowState : States()
    object RedState : States(), FinalState // Machine finishes when enters final state
}

fun main() = runBlocking {
    // Create state machine and configure its states in a setup block
    val machine = createStateMachine(this) {
        addInitialState(GreenState) {
            // Add state listeners
            onEntry { println("Enter green") }
            onExit { println("Exit green") }

            // Setup transition
            transition<SwitchEvent> {
                targetState = YellowState
                // Add transition listener
                onTriggered { println("Transition triggered") }
            }
        }

        addState(YellowState) {
            transition<SwitchEvent>(targetState = RedState)
        }

        addFinalState(RedState)

        onFinished { println("Finished") }
    }

    // Now we can process events
    machine.processEvent(SwitchEvent)
    machine.processEvent(SwitchEvent)
}
```

## Samples

* [Simple Android 2D shooter game sample](https://github.com/nsk90/android-kstatemachine-sample)

  The library itself does not depend on Android.

  <p align="center">
      <img src="https://github.com/nsk90/android-kstatemachine-sample/blob/main/images/android-app-sample.gif"
          alt="Android sample app" width="30%" height="30%"/>
  </p>

* [Transition on FinishedEvent sample](./samples/src/main/kotlin/ru/nsk/samples/FinishedEventSample.kt)
* [FinishedEvent using with DataState sample](./samples/src/main/kotlin/ru/nsk/samples/FinishedEventDataStateSample.kt)
* [Undo transition sample](./samples/src/main/kotlin/ru/nsk/samples/UndoTransitionSample.kt)
* [PlantUML nested states export sample](./samples/src/main/kotlin/ru/nsk/samples/PlantUmlExportSample.kt)
* [Inherit transitions by grouping states sample](./samples/src/main/kotlin/ru/nsk/samples/InheritTransitionsSample.kt)
* [Minimal sealed classes sample](./samples/src/main/kotlin/ru/nsk/samples/MinimalSealedClassesSample.kt)
* [Usage without Kotlin Coroutines sample](./samples/src/main/kotlin/ru/nsk/samples/StdLibMinimalSealedClassesSample.kt)
* [Minimal syntax sample](./samples/src/main/kotlin/ru/nsk/samples/MinimalSyntaxSample.kt)
* [Guarded transition sample](./samples/src/main/kotlin/ru/nsk/samples/GuardedTransitionSample.kt)
* [Cross-level transition sample](./samples/src/main/kotlin/ru/nsk/samples/CrossLevelTransitionSample.kt)
* [Typesafe transition sample](./samples/src/main/kotlin/ru/nsk/samples/TypesafeTransitionSample.kt)
* [Complex syntax sample](./samples/src/main/kotlin/ru/nsk/samples/ComplexSyntaxSample.kt)
  shows many syntax variants and library possibilities, so it looks messy

## Install

KStateMachine is available on `Maven Central` and `JitPack` repositories.

The library consists of 2 components:

* `kstatemachine` - (mandatory) state machine implementation (depends only on Kotlin Standard library)
* `kstatemachine-coroutines` - (optional) add-ons for working with coroutines (depends on Kotlin Coroutines library)

### Maven Central

Add dependencies:

```kotlin
// kotlin
dependencies {
    // Multiplatform artifacts
    implementation("io.github.nsk90:kstatemachine:<Tag>") 
    implementation("io.github.nsk90:kstatemachine-coroutines:<Tag>")
    // or JVM/Android artifacts
    implementation("io.github.nsk90:kstatemachine-jvm:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-jvm:<Tag>")
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

You can see official docs about [dependencies on multiplatform libraries](https://kotlinlang.org/docs/multiplatform-add-dependencies.html#library-used-in-specific-source-sets)

### JitPack

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

## Build

Run `./gradlew build` or build with `Intellij IDEA`.

To run tests from IDE download official [Kotest plugin](https://github.com/kotest/kotest-intellij-plugin).

## Licensed under permissive [Boost Software License](./LICENSE)

## Thanks to supporters

[![Stargazers repo roster for @nsk90/kstatemachine](https://reporoster.com/stars/dark/nsk90/kstatemachine)](https://github.com/nsk90/kstatemachine/stargazers)
[![Forkers repo roster for @nsk90/kstatemachine](https://reporoster.com/forks/dark/nsk90/kstatemachine)](https://github.com/nsk90/kstatemachine/network/members)