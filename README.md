![KStateMachine](./docs/kstatemachine-logo.png)

# KStateMachine

![Build and test with Gradle](https://github.com/nsk90/kstatemachine/workflows/Build%20and%20test%20with%20Gradle/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=nsk90_kstatemachine&metric=alert_status)](https://sonarcloud.io/dashboard?id=nsk90_kstatemachine)
[![codecov](https://codecov.io/gh/nsk90/kstatemachine/branch/master/graph/badge.svg?token=IR2JR43FOZ)](https://codecov.io/gh/nsk90/kstatemachine)
[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.nsk90/kstatemachine?logo=sonatype)](https://central.sonatype.com/artifact/io.github.nsk90/kstatemachine)
[![JitPack](https://img.shields.io/jitpack/version/io.github.nsk90/kstatemachine?style=flat&logo=jitpack&color=brgreen)](https://jitpack.io/#nsk90/kstatemachine)
[![multiplatform support](https://img.shields.io/badge/multiplatform-jvm%20%7C%20android%20%7C%20ios%20%7C%20js%20%7C%20wasm-brightgreen)](https://kstatemachine.github.io/kstatemachine/#multiplatform)

[![Open Collective](https://img.shields.io/badge/open%20collective-kstatemachine-lightblue?logo=opencollective&style=flat)](https://opencollective.com/kstatemachine)
[![JetBrains support](https://img.shields.io/badge/JetBrains-support-black?style=flat&logo=jetbrains)](https://jb.gg/OpenSourceSupport)
[![Mentioned in Awesome Kotlin](https://awesome.re/mentioned-badge.svg)](https://github.com/KotlinBy/awesome-kotlin)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-kstatemachine-green.svg?style=flat)](https://android-arsenal.com/details/1/8276)
[![Share on X](https://img.shields.io/badge/twitter-share-white?logo=x&style=flat)](https://twitter.com/intent/tweet?text=I%20like%20KStateMachine%20library%20%0A%0Ahttps%3A%2F%2Fgithub.com%2Fkstatemachine%2Fkstatemachine&hashtags=kstatemachine,kotlin,opensource)
[![Share on Reddit](https://img.shields.io/badge/reddit-share-red?logo=reddit&style=flat)](https://www.reddit.com/submit?url=https%3A%2F%2Fgithub.com%2Fkstatemachine%2Fkstatemachine&title=I%20like%20KStateMachine%20library)

**[Documentation](https://kstatemachine.github.io/kstatemachine) |
[Sponsors](#sponsors-) |
[Quick start](#quick-start-sample) |
[Samples](#samples) | [Install](#install) |
[Contribution](#contribution) |
[License](#license) |
[Discussions](https://github.com/kstatemachine/kstatemachine/discussions)**

KStateMachine is a Kotlin DSL library for creating [state machines](https://en.wikipedia.org/wiki/Finite-state_machine)
and [statecharts](https://www.sciencedirect.com/science/article/pii/0167642387900359/pdf).

## Overview

Integration features are:

* **Kotlin [DSL](https://kotlinlang.org/docs/type-safe-builders.html#scope-control-dslmarker) syntax.**
  Declarative and clear state machine structure. Using without DSL is also possible.
* **Kotlin [Coroutines](https://kstatemachine.github.io/kstatemachine/pages/multithreading.html#kotlin-coroutines) support.**
  Call suspendable functions within the library.
  You can fully use KStateMachine without Kotlin Coroutines dependency if necessary.
* **Kotlin [Multiplatform](https://kstatemachine.github.io/kstatemachine/pages/multiplatform.html) support.**
* **Zero dependency.** It is written in pure Kotlin, it does not depend on any third party libraries or Android SDK.

State management features:

* **Event based** - [transitions](https://kstatemachine.github.io/kstatemachine/pages/transitions/transitions.html) are performed by
  processing
  incoming events
* **[Reactive](https://kstatemachine.github.io/kstatemachine/pages/states/states.html#listen-states)** - listen for machine, states,
  [state groups](https://kstatemachine.github.io/kstatemachine/pages/states/states.html#listen-group-of-states) and transitions
* **[Guarded](https://kstatemachine.github.io/kstatemachine/pages/transitions/transitions.html#guarded-transitions)
  and [Conditional transitions](https://kstatemachine.github.io/kstatemachine/pages/transitions/transitions.html#conditional-transitions)** - dynamic
  target
  state which is calculated in a moment of event processing depending on application business logic
* **[Nested states](https://kstatemachine.github.io/kstatemachine/pages/states/states.html#nested-states)** - build hierarchical state machines
  (statecharts)
  with [cross-level transitions](https://kstatemachine.github.io/kstatemachine/pages/transitions/transitions.html#cross-level-transitions) support
* **[Composed (nested) state machines](
  https://kstatemachine.github.io/kstatemachine/pages/states/states.html#composed-nested-state-machines
  )** - use state machines as atomic child states
* **[Pseudo states](https://kstatemachine.github.io/kstatemachine/pages/states/pseudo_states.html)** for additional logic in machine
  behaviour
* **[Typesafe transitions](https://kstatemachine.github.io/kstatemachine/pages/transitions/typesafe_transitions.html)** - pass data in
  typesafe way
  from event to state
* **[Parallel states](https://kstatemachine.github.io/kstatemachine/pages/states.html#parallel-states)** - avoid a combinatorial
  explosion of
  states
* **[Undo transitions](https://kstatemachine.github.io/kstatemachine/pages/transitions/transitions.html#undo-transitions)** - navigate back to previous
  state
* **[Optional argument](https://kstatemachine.github.io/kstatemachine/pages/events.html#event-argument)** passing for events and
  transitions
* **[Export](https://kstatemachine.github.io/kstatemachine/pages/export.html)** state machine structure
  to [PlantUML](https://plantuml.com/) and [Mermaid](https://mermaid.js.org/) diagrams
* **[Persist (serialize)](https://kstatemachine.github.io/kstatemachine/pages/persistence.html)**  state machine's active
  configuration and restore it later
* **[Testable](https://kstatemachine.github.io/kstatemachine/pages/testing.html)** - run state machine from specified state and enable internal logging
* **Well tested** - all features are covered
  by [tests](https://github.com/kstatemachine/kstatemachine/tree/master/tests/src/commonTest/kotlin/ru/nsk/kstatemachine)

> [!IMPORTANT]
> SEE FULL [DOCUMENTATION HERE](https://kstatemachine.github.io/kstatemachine)

## Sponsors ❤

I highly appreciate that you donate or become a sponsor to support the project.
Use ❤️ github-sponsors button to see supported methods and push the ⭐ star-button if you like this project.

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

* [Simple Android 2D shooter game sample](https://github.com/kstatemachine/android-kstatemachine-sample)

  The library itself does not depend on Android.

  <p align="center">
      <img src="https://github.com/kstatemachine/android-kstatemachine-sample/blob/main/images/android-app-sample.gif"
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

See [install section in the docs](https://kstatemachine.github.io/kstatemachine/pages/install.html) for details.

### Maven Central

```kotlin
dependencies {
    // multiplatform artifacts, where <Tag> is a library version.
    implementation("io.github.nsk90:kstatemachine:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines:<Tag>")
}
```

## Build

Run `./gradlew build` or build with `Intellij IDEA`.

## Contribution

The library is in a active development phase. You are welcome to propose useful features and contribute to the project.
See [CONTRIBUTING](./CONTRIBUTING.md) file.

## Thanks to supporters

[![Stargazers repo roster for @kstatemachine/kstatemachine](https://reporoster.com/stars/dark/kstatemachine/kstatemachine)](https://github.com/kstatemachine/kstatemachine/stargazers)
[![Forkers repo roster for @kstatemachine/kstatemachine](https://reporoster.com/forks/dark/kstatemachine/kstatemachine)](https://github.com/kstatemachine/kstatemachine/network/members)

## License

Licensed under permissive [Boost Software License](./LICENSE)