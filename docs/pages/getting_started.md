---
layout: page
title: Getting started
nav_order: 0
---

# Getting started
{: .no_toc }

## Page contents
{: .no_toc .text-delta }

- TOC
{:toc}

KStateMachine is a Kotlin Multiplatform library for building hierarchical finite state machines.
You define states, events, and transitions in a DSL; the library manages active state, callback dispatch, and coroutine integration.

## Add the dependency

```kotlin
dependencies {
    implementation("io.github.nsk90:kstatemachine:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines:<Tag>") // recommended
}
```

Where `<Tag>` is the library version. See the [Install](install.html) page for the full platform list and Groovy coordinates.

## Your first state machine

The example below models a traffic light. Three states cycle on a single `SwitchEvent`.
Using a `sealed class` for states is the recommended style — it eliminates `lateinit` references
and makes exhaustive `when` expressions possible.

```kotlin
import kotlinx.coroutines.runBlocking
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.transition.onTriggered

object SwitchEvent : Event

sealed class TrafficLightState : DefaultState() {
    object Green  : TrafficLightState()
    object Yellow : TrafficLightState()
    object Red    : TrafficLightState()
}

fun main() = runBlocking {
    val machine = createStateMachine(this, "Traffic lights") {
        addInitialState(TrafficLightState.Green) {
            onEntry { println("Green — go") }
            onExit  { println("Leaving green") }

            transition<SwitchEvent> {
                targetState = TrafficLightState.Yellow
                onTriggered { println("Transition triggered") }
            }
        }
        addState(TrafficLightState.Yellow) {
            onEntry { println("Yellow — slow down") }
            transition<SwitchEvent> { targetState = TrafficLightState.Red }
        }
        addState(TrafficLightState.Red) {
            onEntry { println("Red — stop") }
            transition<SwitchEvent> { targetState = TrafficLightState.Green }
        }
    }

    machine.processEvent(SwitchEvent) // Green → Yellow
    machine.processEvent(SwitchEvent) // Yellow → Red
    machine.processEvent(SwitchEvent) // Red → Green
}
```

Output:

```
Green — go
Transition triggered
Leaving green
Yellow — slow down
Red — stop
Green — go
```

## Key concepts

| Concept | Role |
|---|---|
| `Event` | Input that drives transitions; define one subclass per logical input |
| `IState` | A node in the machine; holds `onEntry` / `onExit` callbacks |
| `Transition` | Declares which event moves the machine from one state to another |
| `StateMachine` | Root container; processes events and dispatches callbacks |

The machine processes events one at a time in the thread of its `CoroutineScope`.
If an event arrives while another is being processed it is queued automatically by the default
`PendingEventHandler`. See [Events](events.html) for details.

## What to read next

- [State machine](statemachine.html) — creation options, lifecycle, and listeners
- [States](states/states.html) — nested states, parallel states, final states, data states
- [Transitions](transitions/transitions.html) — guards, conditional transitions, undo, cross-level
- [Events](events.html) — processing results, pending events, ignored event handling
- [Install](install.html) — full dependency coordinates for all platforms
