---
layout: page
title: Meta information
---

# Meta information

{: .no_toc }

## Page contents

{: .no_toc .text-delta }

- TOC
  {:toc}

## `metaInfo` property

The library declares a `metaInfo` property on `IState` and `Transition`.
`MetaInfo` is a marker interface that lets you attach static, immutable information to any state or transition.
The mechanism is open: you can define your own `MetaInfo` sub-interfaces alongside the built-in ones.

{: .note }
`MetaInfo` is considered to be immutable data by design

### Custom `MetaInfo` example

A common use case is tagging states with analytics screen names without coupling the state machine to an analytics SDK:

```kotlin
interface AnalyticsTag : MetaInfo {
    val screenName: String
}

fun analyticsTag(name: String) = object : AnalyticsTag {
    override val screenName = name
}

val machine = createStateMachine(scope) {
    state("checkout") {
        metaInfo = analyticsTag("screen_checkout")
        // ...
    }

    addListener(object : StateMachine.Listener {
        override suspend fun onStateEntry(state: IState, transitionParams: TransitionParams<*>) {
            val tag = state.metaInfo as? AnalyticsTag
            tag?.let { analytics.track(it.screenName) }
        }
    })
}
```

## `MetaInfo` composition

If you need to attach more than one `MetaInfo` to a state or transition you have two options:

1. Use `CompositeMetaInfo`, constructed with `buildCompositeMetaInfo {}`. It holds a set of `MetaInfo` objects
   and is the recommended approach when combining library-provided and custom metadata:

   ```kotlin
   state("checkout") {
       metaInfo = buildCompositeMetaInfo {
           add(analyticsTag("screen_checkout"))
           add(buildUmlMetaInfo { umlLabel = "Checkout flow" })
       }
   }
   ```

   Limitations:
    * `CompositeMetaInfo` cannot be nested. Only one layer is supported.
    * Each `MetaInfo` subtype may appear at most once — an exception is thrown otherwise.

2. Manually implement all required `MetaInfo` interfaces in a single object. Useful when you control all
   the interfaces involved and want to avoid the composite wrapper.

## Built-in `MetaInfo` subclasses

| Type                                         | Purpose                                                                                                   |
|----------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| `UmlMetaInfo`                                | Adds labels, descriptions, and notes to export diagrams. Built with `buildUmlMetaInfo {}`.                |
| `ExportMetaInfo`                             | Provides resolution hints for conditional transitions during export. Built with `buildExportMetaInfo {}`. |
| `IgnoreUnsafeCallConditionalLambdasMetaInfo` | Suppresses `unsafeCallConditionalLambdas` execution for a specific state or transition during export.     |

See [controlling export output](https://kstatemachine.github.io/kstatemachine/pages/export.html#controlling-export-output)
for usage details.

For the full `MetaInfo` type hierarchy see the
[KDoc API reference](https://kstatemachine.github.io/kstatemachine/kdoc/index.html).