---
layout: page
title: Export
---

# Export

{: .no_toc }

## Page contents

{: .no_toc .text-delta }

- TOC
  {:toc}

The library supports export into PlantUML and Mermaid diagram drawing systems. They both use PlantUML text format.
Mermaid supports fewer features then PlantUML itself.
Please note that both of them have their own limitations and corner cases.

{: .note }
Transitions that use lambdas like `transitionConditionally()` and `transitionOn()` or `choiceState()` etc.,
are not exported by default.
See [export with unsafeCallConditionalLambdas flag](#export-with-unsafecallconditionallambdas-flag) section how to
handle such constructions.

## PlantUML

Use `exportToPlantUml()`/`exportToPlantUmlBlocking()` extension function to export state machine
to [PlantUML state diagram](https://plantuml.com/en/state-diagram).
`showEventLabels` flag allows to include `Event` types into the output.

```kotlin
val machine = createStateMachine(scope) { /* ... */ }
println(machine.exportToPlantUml())
```

Copy/paste resulting output to [Plant UML online editor](http://www.plantuml.com/plantuml/)

See [PlantUML nested states export sample](https://github.com/KStateMachine/kstatemachine/tree/master/samples/src/commonMain/kotlin/ru/nsk/samples/PlantUmlExportSample.kt)

## Mermaid

`Mermaid` uses almost the same text format as `PlantUML` for compatibility reasons.

Use `exportToMermaid()`/`exportToToMermaidBlocking()` extension function to export state machine
to [Mermaid state diagram](https://mermaid.js.org/syntax/stateDiagram.html).
`showEventLabels` flag allows to include `Event` types into the output.

```kotlin
val machine = createStateMachine(scope) { /* ... */ }
println(machine.exportToMermaid())
```

* `Intellij IDEA` users may use official [Mermaid plugin](https://plugins.jetbrains.com/plugin/20146-mermaid)
  to view diagrams directly in IDE for file types: `.mmd` and `.mermaid`.
* or copy/paste resulting output to [Mermaid live editor](https://mermaid.live/)

See [Mermaid nested states export sample](https://github.com/KStateMachine/kstatemachine/tree/master/samples/src/commonMain/kotlin/ru/nsk/samples/MermaidExportSample.kt)

## Controlling export output

To beautify and enrich export output, you can use `UmlMetaInfo` for both `IState` and `Transition`. It can be built
with `buildUmlMetaInfo()` function.
It allows to add notes and labels to your state diagrams.

```kotlin
state("State1") {
    metaInfo = buildUmlMetaInfo {
        umlLabel = "State 1 long label"
        umlStateDescriptions = listOf("Description 1", "Description 2")
        umlNotes = listOf("Note 1", "Note 2")
    }
}
```

See [PlantUML with MetaInfo export sample](https://github.com/KStateMachine/kstatemachine/tree/master/samples/src/commonMain/kotlin/ru/nsk/samples/PlantUmlExportWithMetaInfoSample.kt)

## Export with `unsafeCallConditionalLambdas` flag

Transitions that use lambdas like `transitionConditionally()` and `transitionOn()` or `choiceState()` etc.,
are not exported by default.

You can enable their export with `unsafeCallConditionalLambdas` flag of `exportToPlantUml()`/`exportToMermaid()`
functions.
With `unsafeCallConditionalLambdas` flag set, user defined lambdas that are passed to the library to calculate next
state would be called during export process. This will give more complete (still not full) export output,
but may cause runtime errors depending on what the lambda actually do. As it may touch application data that is not
valid when export is running, also `event` argument will be faked with `ExportPlantUmlEvent` by unsafe cast, so touching
it will cause `ClassCastException`.

That is why `unsafeCallConditionalLambdas` flag should be considered as debug/development tool only.

The library provides additional `MetaInfo` objects that might be used along with `unsafeCallConditionalLambdas` flag
to provide complete output (with a help of a user).

* `IgnoreUnsafeCallConditionalLambdasMetaInfo` allows to ignore `unsafeCallConditionalLambdas` flag for some state or
  transition. Corresponding lambda will not be executed.
* `ExportMetaInfo` (is built by `buildExportMetaInfo` function) allows a user to manually specify hint
  information (list of `ResolutionHint` objects) for the library to print complete export output.
  User is responsible to specify correct information.

### Resolution hints

* There is `StateResolutionHint` which is useful to specify target states. So lambda execution is not needed.
  This hint works even without `unsafeCallConditionalLambdas` flag.
* And `EventAndArgumentResolutionHint` allowing to specify `Event` and argument, which will be used to execute a
  conditional lambda. This allows to bypass limitations of default behaviour with fake `ExportPlantUmlEvent`.
  This hint works only if `unsafeCallConditionalLambdas` flag is `true`.

Here are some samples using resolution hints:

```kotlin
class ValueEvent(val value: Int) : Event

// ...
transitionConditionally<ValueEvent> {
    direction = {
        when (event.value) {
            1 -> targetState(state1)
            2 -> targetState(state2)
            3 -> targetParallelStates(state1, state2)
            4 -> stay()
            else -> noTransition()
        }
    }
    metaInfo = buildExportMetaInfo {
        resolutionHints = setOf(
            // the library does not need to call "direction" lambda, this hint provides the result (state1) directly
            StateResolutionHint("when 1", state1),
            // calls "direction" lambda during export with specified Event and optional argument (lambda will return state2)
            EventAndArgumentResolutionHint("when 2", ValueEvent(2)),
            // you can specify set of states that represents parallel target states
            StateResolutionHint("when 3", setOf(state1, state2)),
            // describes stay() behaviour without calling "direction" lambda
            StateResolutionHint("when 4", this@createStateMachine),
            // resolves to stay() by calling "direction" lambda
            EventAndArgumentResolutionHint("when 4", ValueEvent(4)),
            // useless, does not affect export output as it resolves to noTransition()
            EventAndArgumentResolutionHint("else", ValueEvent(5)),
        )
    }
}
```