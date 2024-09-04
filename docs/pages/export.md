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
You can enable their export with `unsafeCallConditionalLambdas` flag of `exportToPlantUml()`/`exportToMermaid()`
functions.
With `unsafeCallConditionalLambdas` flag set, user defined lambdas that are passed to the library to calculate next
state would be called during export process. This will give more complete (still not full) export output,
but may cause runtime errors depending on what the lambda actually do. As it may touch application data that is not
valid when export is running, also `event` argument will be faked by unsafe cast, so touching it
will cause `ClassCastException`
That is why `unsafeCallConditionalLambdas` flag should be considered as debug/development tool only.

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
with `buildUmlMetaInfo()` function:

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

