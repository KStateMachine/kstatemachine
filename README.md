# KStateMachine

![Build and test with Gradle](https://github.com/nsk90/kstatemachine/workflows/Build%20and%20test%20with%20Gradle/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=nsk90_kstatemachine&metric=alert_status)](https://sonarcloud.io/dashboard?id=nsk90_kstatemachine)
[![](https://jitpack.io/v/nsk90/kstatemachine.svg)](https://jitpack.io/#nsk90/kstatemachine)
![Dependencies none](https://img.shields.io/badge/dependencies-none-green)
[![codecov](https://codecov.io/gh/nsk90/kstatemachine/branch/master/graph/badge.svg?token=IR2JR43FOZ)](https://codecov.io/gh/nsk90/kstatemachine)

KStateMachine is a Kotlin DSL library for creating finite state
machines ([FSM](https://en.wikipedia.org/wiki/Finite-state_machine)) and hierarchical state machines
(HSM).

## Overview

Main features are:

* Zero dependency. It is written in pure Kotlin, it does not depend on any other libraries or Android SDK
* Kotlin DSL syntax for defining state machine structure. Using without DSL is also possible
* Event based - transitions are performed by processing incoming events
* Listeners for states and transitions
* [Guarded](./doc/detailed_doc.md#guarded-transitions)
  and [Conditional transitions](./doc/detailed_doc.md#conditional-transitions) with dynamic target state which
  is calculated in a moment of event processing depending on application business logic
* [Nested states](./doc/detailed_doc.md#nested-states) - hierarchical state machines (HSMs)
  with [cross level transitions](./doc/detailed_doc.md#cross-level-transitions) support
* [Composed (nested) state machines.](./doc/detailed_doc.md#composed-(nested)-state-machines) Use state machines
  as atomic child states
* [Typesafe transitions](./doc/detailed_doc.md#typesafe-transitions) to pass data in typesafe way from event to
  state
* [Parallel states](./doc/detailed_doc.md#parallel-states) to avoid a combinatorial explosion of states
* [Argument](./doc/detailed_doc.md#arguments) passing for events and transitions
* [Export state machine](./doc/detailed_doc.md#export) structure to [PlantUML](https://plantuml.com/);
* Built-in [logging](./doc/detailed_doc.md#logging) support

_The library is currently in a development phase. You are welcome to propose useful features._

## SEE [WIKI PAGE](https://github.com/nsk90/kstatemachine/wiki) FOR MORE INFO

## [Android sample app](https://github.com/nsk90/android-kstatemachine-sample)

The library itself does not depend on Android.

<p align="center">
    <img src="https://github.com/nsk90/android-kstatemachine-sample/blob/main/images/android-app-sample.gif"
        alt="Android sample app" width="300"/>
</p>

## Quick start sample (finishing traffic light)

![Traffic light diagram](./doc/diagrams/finishing-traffic-light.png)

```kotlin
// Define events
sealed class Events {
    object YellowEvent : Event
    object RedEvent : Event
}

// Define states
sealed class States {
    object GreenState : DefaultState("Green")
    object YellowState : DefaultState("Yellow")
    object RedState : DefaultFinalState("Red") // State machine finishes when enters final state
}

fun main() {
    // Create state machine and configure its states in a setup block
    val machine = createStateMachine {
        addInitialState(States.GreenState) {
            // Add state listeners
            onEntry { println("Enter $name state") }
            onExit { println("Exit $name state") }

            // Setup transition on YellowEvent
            transition<Events.YellowEvent> {
                targetState = States.YellowState
                // Add transition listener
                onTriggered { println("Transition on ${it.event}") }
            }
        }

        addState(States.YellowState) {
            transition<Events.RedEvent> { targetState = States.RedState }
        }

        addFinalState(States.RedState)
    }

    // Process events
    machine.processEvent(Events.YellowEvent)
    machine.processEvent(Events.RedEvent)
}
```

## Samples

* [Simple Android 2D shooter game sample](https://github.com/nsk90/android-kstatemachine-sample)
* [PlantUML nested states export sample](./samples/src/main/kotlin/ru/nsk/samples/PlantUmlExportSample.kt)
* [Inherit transitions by grouping states sample](./samples/src/main/kotlin/ru/nsk/samples/InheritTransitionsSample.kt)
* [Minimal sealed classes sample](./samples/src/main/kotlin/ru/nsk/samples/MinimalSealedClassesSample.kt)
* [Minimal syntax sample](./samples/src/main/kotlin/ru/nsk/samples/MinimalSyntaxSample.kt)
* [Guarded transition sample](./samples/src/main/kotlin/ru/nsk/samples/GuardedTransitionSample.kt)
* [Cross level transition sample](./samples/src/main/kotlin/ru/nsk/samples/CrossLevelTransitionSample.kt)
* [Typesafe transition sample](./samples/src/main/kotlin/ru/nsk/samples/TypesafeTransitionSample.kt)
* [Complex syntax sample](./samples/src/main/kotlin/ru/nsk/samples/ComplexSyntaxSample.kt)
  shows many syntax variants and library possibilities, so it looks messy

## Install

Add the [JitPack](https://jitpack.io/#nsk90/kstatemachine/Tag) repository to your build file. Add it in your
root `build.gradle` at the end of repositories:

```groovy
allprojects {
    repositories {
        //  ...
        maven { url 'https://jitpack.io' }
    }
}
```

Add the dependency:

```groovy
dependencies {
    implementation 'com.github.nsk90:kstatemachine:<Tag>'
}
```

Where `<Tag>` is a library version.

## Build

Run `./gradlew build` or build with `Intellij IDEA`.

To run tests from IDE download official [Kotest plugin](https://github.com/kotest/kotest-intellij-plugin).

## Licensed under the [Boost Software License](./LICENSE)