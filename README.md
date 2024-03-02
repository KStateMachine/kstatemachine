![KStateMachine](./docs/kstatemachine-logo.png)

# KStateMachine

![Build and test with Gradle](https://github.com/nsk90/kstatemachine/workflows/Build%20and%20test%20with%20Gradle/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=nsk90_kstatemachine&metric=alert_status)](https://sonarcloud.io/dashboard?id=nsk90_kstatemachine)
[![codecov](https://codecov.io/gh/nsk90/kstatemachine/branch/master/graph/badge.svg?token=IR2JR43FOZ)](https://codecov.io/gh/nsk90/kstatemachine)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.nsk90/kstatemachine)](https://central.sonatype.com/artifact/io.github.nsk90/kstatemachine)
[![JitPack](https://jitpack.io/v/nsk90/kstatemachine.svg)](https://jitpack.io/#nsk90/kstatemachine)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-KStateMachine-green.svg?style=flat)](https://android-arsenal.com/details/1/8276)
[![Mentioned in Awesome Kotlin](https://awesome.re/mentioned-badge.svg)](https://github.com/KotlinBy/awesome-kotlin)
[![Open Collective](https://img.shields.io/badge/opencollective-member-lightblue?logo=opencollective&style=flat)](https://opencollective.com/kstatemachine)
[![multiplatform support](https://img.shields.io/badge/multiplatform-jvm%20%7C%20android%20%7C%20ios-brightgreen)](https://kstatemachine.github.io/kstatemachine/#multiplatform)
[![Share on X](https://img.shields.io/badge/twitter-share-white?logo=x&style=flat)](https://twitter.com/intent/tweet?text=I%20like%20KStateMachine%20library%20%0A%0Ahttps%3A%2F%2Fgithub.com%2Fnsk90%2Fkstatemachine%0A%0A%23kstatemachine)
[![Share on Reddit](https://img.shields.io/badge/reddit-share-red?logo=reddit&style=flat)](https://www.reddit.com/submit?url=https%3A%2F%2Fgithub.com%2Fnsk90%2Fkstatemachine&title=I%20like%20KStateMachine%20library)


[Documentation](https://kstatemachine.github.io/kstatemachine) | [Quick start](#quick-start-sample) | [Samples](#samples) | [Install](#install) | [License](#license) | [Discussions](https://github.com/nsk90/kstatemachine/discussions)

KStateMachine is a Kotlin DSL library for creating [state machines](https://en.wikipedia.org/wiki/Finite-state_machine)
and [statecharts](https://www.sciencedirect.com/science/article/pii/0167642387900359/pdf).

## Overview

Integration features are:

* **Kotlin [DSL](https://kotlinlang.org/docs/type-safe-builders.html#scope-control-dslmarker) syntax.**
  Declarative and clear state machine structure. Using without DSL is also possible.
* **Kotlin [Coroutines](https://kstatemachine.github.io/kstatemachine/#kotlin-coroutines) support.**
  Call suspendable functions within the library.
  You can fully use KStateMachine without Kotlin Coroutines dependency if necessary.
* **Kotlin [Multiplatform](https://kstatemachine.github.io/kstatemachine/#multiplatform) support.**
* **Zero dependency.** It is written in pure Kotlin, it does not depend on any third party libraries or Android SDK.

State management features:

* **Event based** - [transitions](https://kstatemachine.github.io/kstatemachine/#setup-transitions) are performed by processing
  incoming events
* **[Reactive](https://kstatemachine.github.io/kstatemachine/#listen-states)** - listen for machine, states,
  [state groups](https://kstatemachine.github.io/kstatemachine/#listen-group-of-states) and transitions
* **[Guarded](https://kstatemachine.github.io/kstatemachine/#guarded-transitions)
  and [Conditional transitions](https://kstatemachine.github.io/kstatemachine/#conditional-transitions)** - dynamic target
  state which is calculated in a moment of event processing depending on application business logic
* **[Nested states](https://kstatemachine.github.io/kstatemachine/#nested-states)** - build hierarchical state machines
  (statecharts)
  with [cross-level transitions](https://kstatemachine.github.io/kstatemachine/#cross-level-transitions) support
* **[Composed (nested) state machines.](https://kstatemachine.github.io/kstatemachine/#composed-(nested)-state-machines)** Use
  state machines as atomic child states
* **[Pseudo states](https://kstatemachine.github.io/kstatemachine/#pseudo-states)** for additional logic in machine behaviour
* **[Typesafe transitions](https://kstatemachine.github.io/kstatemachine/#typesafe-transitions)** - pass data in typesafe way
  from event to state
* **[Parallel states](https://kstatemachine.github.io/kstatemachine/#parallel-states)** - avoid a combinatorial explosion of
  states
* **[Undo transitions](https://kstatemachine.github.io/kstatemachine/#undo-transitions)** - navigate back to previous state
* **[Optional argument](https://kstatemachine.github.io/kstatemachine/#optinal-arguments)** passing for events and transitions
* **[Export](https://kstatemachine.github.io/kstatemachine/#export)** state machine structure
  to [PlantUML](https://plantuml.com/) and [Mermaid](https://mermaid.js.org/) diagrams
* **[Logging](https://kstatemachine.github.io/kstatemachine/#logging)** - useful for debugging
* **[Testable](https://kstatemachine.github.io/kstatemachine/#testing)** - run state machine from specified state
* **Well tested** - all features are covered
  by [tests](https://github.com/nsk90/kstatemachine/tree/master/tests/src/commonTest/kotlin/ru/nsk/kstatemachine)

> [!IMPORTANT]
> SEE FULL [DOCUMENTATION HERE](https://kstatemachine.github.io/kstatemachine)

> [!NOTE]
> The library is in a development phase. You are welcome to propose useful features, or contribute to the project.
> 
> Don't forget to push the ⭐ if you like this project.
> 
> You can donate or become a sponsor to support the project, using ❤️ github-sponsors button.

## Quick start sample

### Finishing traffic light

```mermaid
stateDiagram-v2
    direction LR
    [*] --> GreenState
    GreenState --> YellowState: SwitchEvent
    YellowState --> RedState: SwitchEvent
    RedState --> [*]

```

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
* [Finished state sample](./samples/src/commonMain/kotlin/ru/nsk/samples/FinishedStateSample.kt)
* [Transition on FinishedEvent sample](./samples/src/commonMain/kotlin/ru/nsk/samples/FinishedEventSample.kt)
* [FinishedEvent using with DataState sample](./samples/src/commonMain/kotlin/ru/nsk/samples/FinishedEventDataStateSample.kt)
* [Undo transition sample](./samples/src/commonMain/kotlin/ru/nsk/samples/UndoTransitionSample.kt)
* [PlantUML nested states export sample](./samples/src/commonMain/kotlin/ru/nsk/samples/PlantUmlExportSample.kt)
* [Mermaid nested states export sample](./samples/src/commonMain/kotlin/ru/nsk/samples/MermaidExportSample.kt)
* [PlantUML with MetaInfo export sample](./samples/src/commonMain/kotlin/ru/nsk/samples/PlantUmlExportWithMetaInfoSample.kt)
* [Inherit transitions by grouping states sample](./samples/src/commonMain/kotlin/ru/nsk/samples/InheritTransitionsSample.kt)
* [Minimal sealed classes sample](./samples/src/commonMain/kotlin/ru/nsk/samples/MinimalSealedClassesSample.kt)
* [Usage without Kotlin Coroutines sample](./samples/src/commonMain/kotlin/ru/nsk/samples/StdLibMinimalSealedClassesSample.kt)
* [Minimal syntax sample](./samples/src/commonMain/kotlin/ru/nsk/samples/MinimalSyntaxSample.kt)
* [Guarded transition sample](./samples/src/commonMain/kotlin/ru/nsk/samples/GuardedTransitionSample.kt)
* [Cross-level transition sample](./samples/src/commonMain/kotlin/ru/nsk/samples/CrossLevelTransitionSample.kt)
* [Typesafe transition sample](./samples/src/commonMain/kotlin/ru/nsk/samples/TypesafeTransitionSample.kt)
* [Complex syntax sample](./samples/src/commonMain/kotlin/ru/nsk/samples/ComplexSyntaxSample.kt)
  shows many syntax variants and library possibilities, so it looks messy

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

## Build

Run `./gradlew build` or build with `Intellij IDEA`.

To run tests from IDE download official [Kotest plugin](https://github.com/kotest/kotest-intellij-plugin).

## License

Licensed under permissive [Boost Software License](./LICENSE)

## Thanks to supporters

[![Stargazers repo roster for @nsk90/kstatemachine](https://reporoster.com/stars/dark/nsk90/kstatemachine)](https://github.com/nsk90/kstatemachine/stargazers)
[![Forkers repo roster for @nsk90/kstatemachine](https://reporoster.com/forks/dark/nsk90/kstatemachine)](https://github.com/nsk90/kstatemachine/network/members)