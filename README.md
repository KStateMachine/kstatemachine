![KStateMachine](./doc/kstatemachine-logo.png)

# KStateMachine

![Build and test with Gradle](https://github.com/nsk90/kstatemachine/workflows/Build%20and%20test%20with%20Gradle/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=nsk90_kstatemachine&metric=alert_status)](https://sonarcloud.io/dashboard?id=nsk90_kstatemachine)
[![](https://jitpack.io/v/nsk90/kstatemachine.svg)](https://jitpack.io/#nsk90/kstatemachine)
![Maven Central](https://img.shields.io/maven-central/v/io.github.nsk90/kstatemachine)
![Dependencies none](https://img.shields.io/badge/dependencies-none-green)
[![codecov](https://codecov.io/gh/nsk90/kstatemachine/branch/master/graph/badge.svg?token=IR2JR43FOZ)](https://codecov.io/gh/nsk90/kstatemachine)
[![Android Arsenal]( https://img.shields.io/badge/Android%20Arsenal-KStateMachine-green.svg?style=flat )]( https://android-arsenal.com/details/1/8276 )

KStateMachine is a Kotlin DSL library for creating finite state
machines ([FSM](https://en.wikipedia.org/wiki/Finite-state_machine)) and hierarchical state machines
(HSM).

## Overview

Main features are:

* Zero dependency. It is written in pure Kotlin, it does not depend on any other libraries or Android SDK
* Kotlin DSL syntax for defining state machine structure. Using without DSL is also possible
* Backward compatible till Kotlin 1.4
* Event based - transitions are performed by processing incoming events
* Listeners for machine, states, state groups and transitions. Listener callbacks are shipped with information about
  current transition
* [Guarded](https://github.com/nsk90/kstatemachine/wiki#guarded-transitions)
  and [Conditional transitions](https://github.com/nsk90/kstatemachine/wiki#conditional-transitions) with dynamic target
  state which is calculated in a moment of event processing depending on application business logic
* [Nested states](https://github.com/nsk90/kstatemachine/wiki#nested-states) - hierarchical state machines (HSMs)
  with [cross level transitions](https://github.com/nsk90/kstatemachine/wiki#cross-level-transitions) support
* [Composed (nested) state machines.](https://github.com/nsk90/kstatemachine/wiki#composed-(nested)-state-machines) Use
  state machines as atomic child states
* [Typesafe transitions](https://github.com/nsk90/kstatemachine/wiki#typesafe-transitions) to pass data in typesafe way
  from event to state
* [Parallel states](https://github.com/nsk90/kstatemachine/wiki#parallel-states) to avoid a combinatorial explosion of
  states
* [Argument](https://github.com/nsk90/kstatemachine/wiki#arguments) passing for events and transitions
* [Export state machine](https://github.com/nsk90/kstatemachine/wiki#export) structure
  to [PlantUML](https://plantuml.com/);
* Built-in [logging](https://github.com/nsk90/kstatemachine/wiki#logging) support

_The library is currently in a development phase. You are welcome to propose useful features._
_Don't forget to push the ⭐ if you like this project._

## SEE FULL [DOCUMENTATION HERE](https://nsk90.github.io/kstatemachine)

## Quick start sample (finishing traffic light)

![Traffic light diagram](./doc/diagrams/finishing-traffic-light.png)

```kotlin
sealed class Events {
    object NextEvent : Event
}

sealed class States {
    object GreenState : DefaultState()
    object YellowState : DefaultState()
    object RedState : DefaultFinalState() // Machine finishes when enters final state
}

fun main() {
    // Create state machine and configure its states in a setup block
    val machine = createStateMachine {
        addInitialState(GreenState) {
            // Add state listeners
            onEntry { println("Enter green") }
            onExit { println("Exit green") }

            // Setup transition
            transition<NextEvent> {
                targetState = YellowState
                // Add transition listener
                onTriggered { println("Transition triggered") }
            }
        }

        addState(YellowState) {
            transition<NextEvent>(targetState = RedState)
        }

        addFinalState(RedState)

        onFinished { println("Finished") }
    }

    // Now we can process events
    machine.processEvent(NextEvent)
    machine.processEvent(NextEvent)
}
```

## Samples

* [Simple Android 2D shooter game sample](https://github.com/nsk90/android-kstatemachine-sample)

  The library itself does not depend on Android.

  <p align="center">
      <img src="https://github.com/nsk90/android-kstatemachine-sample/blob/main/images/android-app-sample.gif"
          alt="Android sample app" width="30%" height="30%"/>
  </p>

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

KStateMachine is available on Maven Central and JitPack repositories.

### Maven Central

Add the dependency:

```groovy
dependencies {
    implementation 'io.github.nsk90:kstatemachine:<Tag>'
}
```

Where `<Tag>` is a library version.

### JitPack

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