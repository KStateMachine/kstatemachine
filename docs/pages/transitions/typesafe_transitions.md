---
layout: page
title: Typesafe transitions
parent: Transitions
---

# Typesafe transitions
{: .no_toc }

## Page contents
{: .no_toc .text-delta }

- TOC
{:toc}

It is a common case when a state expects to receive some data from an event. Library provides typesafe API for such
case. It is implemented with `DataEvent` and `DataState`. Both interfaces are parameterized with data type. To create
typesafe transition use `dataTransition()` and `dataTransitionOn()` functions. This API helps to ensure that event data
parameter type matches data parameter type that is expected by a target state of a transition. Compiler will protect you
from defining a transition with incompatible data type parameters of event and target state.

```kotlin
class StringEvent(override val data: String) : DataEvent<String>

createStateMachine(scope) {
    val state2 = dataState<String> {
        onEntry { println("State data: $data") }
    }

    initialState {
        dataTransition<StringEvent, String> { targetState = state2 }
    }
}
```

`DataState`'s `data` field is set and might be accessed _only_ while the state is active. At the moment when `DataState`
is activated it requires data value from a `DataEvent`. You can use `lastData` field to access last data value even
after state exit, it falls back to `defaultData` if provided or throws.

### MutableDataState

In some cases it might be necessary to change `DataState`'s data manually. To archive it the library introduces 
additional `MutableDataState` type, which allows `data` field mutation with `setData` method. 
This is more flexible but less strict approach than using simple `DataState` which allows `data` field update
only by a transition.

### Target-less data transitions

You can define target-less transitions for `DataState`. Please, note that if you want such transition to change state's
`data` field, it should be `EXTERNAL` type. If target-less transition is `LOCAL` it does not change states data.
This is related to the way how `DataState` is implemented, `data` field is changed only on state entry moment.

### Corner cases of `DataState` activation

1. Implicit activation. `DataState` might be activated by `Event` (not `DataEvent`) that is targeting its child state.
   In this case `data` field of `DataState` is assigned with `lastData` field value.
   If state is activating the first time `lastData` falls back to `defaultData` if provided, otherwise exception is
   thrown. Starting from library version `v0.34.0` you can additionally customize implicit `DataState` activation
   with `DataExtractor::extract` method, which receives `isImplicitActivation` argument set to `true` in this case.
   You can find a sample of such behaviour in [TypesafeTransitionTest](https://github.com/KStateMachine/kstatemachine/blob/master/tests/src/commonTest/kotlin/ru/nsk/kstatemachine/transition/TypesafeTransitionTest.kt)
   While it might be useful that is not typesafe and should be used with caution.
2. Activation by `undo()` of `UndoEvent`. This works same way as undone transition.
3. Activation by `FinishedEvent`. `FinishedEvent` may contain non-null data field. `DataState` receives this data
   if its type matches. `DataExtractor` class is responsible for matching. Such transition might be created only by
   `transitionConditionally()` function.
4. Activation by non data event. This should not be necessary, but it might be done manually, same way as in case 3.
   Using custom `DataExtractor` you can pass any data from any event type to `DataState`, even by implicit activation.

### Known issues

It is not recommended to use generic classes as events and as argument of `DataState`. JVM removes
difference between generic classes with different argument types, this is known as type erasure.
So library cannot separate such types from each other at runtime. When it is necessary to check that some object is an
instance of
a class, such check may be positive for class parameterized with any type.
So it's easier avoid using generic types in such cases. You have to use custom `EventMatcher`s and `DataExtractor`'s
that
will use some additional information to compare such types, or be sure that such invalid comparison never happens.