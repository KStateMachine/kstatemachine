---
layout: page
title: Samples
---

# Samples

{: .no_toc }

## Page contents

{: .no_toc .text-delta }

- TOC
  {:toc}

## Full app samples

### Compose 2D shooter game

[compose-kstatemachine-sample](https://github.com/KStateMachine/compose-kstatemachine-sample) —
a complete Compose Multiplatform game that uses KStateMachine to drive game-object behaviour.

### Android 2D shooter game (deprecated)

[android-kstatemachine-sample](https://github.com/kstatemachine/android-kstatemachine-sample) —
the original Android version. Note: KStateMachine itself has no Android dependency; this sample
demonstrates how the library integrates with an Android project.

## Code samples

All samples live under
[
`samples/src/commonMain/kotlin/ru/nsk/samples/`](https://github.com/KStateMachine/kstatemachine/tree/master/samples/src/commonMain/kotlin/ru/nsk/samples)
in the repository.

### Getting started

| Sample                                                                                                                                                                          | What it shows                              |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------|
| [MinimalSyntaxSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/MinimalSyntaxSample.kt)                           | Simplest possible machine                  |
| [MinimalSealedClassesSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/MinimalSealedClassesSample.kt)             | Idiomatic sealed-class states              |
| [StdLibMinimalSealedClassesSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/StdLibMinimalSealedClassesSample.kt) | Usage without Kotlin Coroutines dependency |
| [ComplexSyntaxSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/ComplexSyntaxSample.kt)                           | Kitchen-sink demo of many syntax variants  |

### Transitions

| Sample                                                                                                                                                                        | What it shows                               |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|
| [TypesafeTransitionSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/TypesafeTransitionSample.kt)               | Pass typed data from event to target state  |
| [GuardedTransitionSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/GuardedTransitionSample.kt)                 | Conditional / guarded transitions           |
| [CrossLevelTransitionSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/CrossLevelTransitionSample.kt)           | Transitions across nested state levels      |
| [InheritTransitionsSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/InheritTransitionsSample.kt)               | Share transitions by grouping states        |
| [UndoTransitionSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/UndoTransitionSample.kt)                       | Navigate backwards (undo)                   |
| [TargetlessTransitionSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/TargetlessTransitionSample.kt)           | Internal transitions without a state change |
| [LocalVsExternalTransitionSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/LocalVsExternalTransitionSample.kt) | LOCAL vs EXTERNAL transition semantics      |

### States

| Sample                                                                                                                                                                    | What it shows                                    |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------|
| [FinishedStateSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/FinishedStateSample.kt)                     | Final states and machine completion              |
| [FinishedEventSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/FinishedEventSample.kt)                     | Reacting to `FinishedEvent`                      |
| [FinishedEventDataStateSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/FinishedEventDataStateSample.kt)   | `FinishedEvent` combined with `DataState`        |
| [MutableDataStateSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/MutableDataStateSample.kt)               | Updating state data without a new event          |
| [HistoryStateSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/HistoryStateSample.kt)                       | Restore previously active sub-state on re-entry  |
| [ChoiceStateSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/ChoiceStateSample.kt)                         | Route to a state dynamically via a pseudo-state  |
| [ComposedMachinesSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/ComposedMachinesSample.kt)               | Nest a state machine as a child state            |
| [ParallelRegionListenersSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/ParallelRegionListenersSample.kt) | Listen for all-of / any-of parallel state combos |

### Persistence & export

| Sample                                                                                                                                                                                                  | What it shows                                     |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|
| [SerializationEventRecordingSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/SerializationEventRecordingSample.kt)                       | Record events and restore state via serialization |
| [PlantUmlExportSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/PlantUmlExportSample.kt)                                                 | Export machine structure to PlantUML              |
| [MermaidExportSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/MermaidExportSample.kt)                                                   | Export machine structure to Mermaid               |
| [PlantUmlExportWithUmlMetaInfoSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/PlantUmlExportWithUmlMetaInfoSample.kt)                   | Export with UML metadata annotations              |
| [PlantUmlUnsafeExportWithExportMetaInfoSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/PlantUmlUnsafeExportWithExportMetaInfoSample.kt) | Unsafe export with `ExportMetaInfo`               |

### Coroutines & observation

| Sample                                                                                                                                                              | What it shows                             |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| [FlowObservationSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/FlowObservationSample.kt)           | Observe state changes via Kotlin `Flow`   |
| [AsyncEventProcessingSample](https://github.com/KStateMachine/kstatemachine/blob/master/samples/src/commonMain/kotlin/ru/nsk/samples/AsyncEventProcessingSample.kt) | Fire-and-forget / deferred event dispatch |
