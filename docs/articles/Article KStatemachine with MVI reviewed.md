# Integrating State Machines with MVI Architecture in Kotlin for Reactive Android Apps.

In this article, we’ll explore how two powerful technologies MVI architecture and state machines can work together seamlessly. Both follow a reactive approach, utilize events, and maintain state that reflects application data, making their integration particularly effective for building robust Android applications. 

We’ll demonstrate how these technologies complement each other by creating a simple Android game sample. This project will showcase how to leverage the strengths of MVI and state machines to manage dynamic application states effectively.

To implement MVI, we’ll avoid using any external frameworks, opting instead for a minimal, custom solution tailored to our needs. For state machine functionality, we’ll use my library, **[KStateMachine](https://github.com/nsk90/kstatemachine)**.

This approach ensures both simplicity and clarity, allowing us to focus on understanding and applying the core principles of these technologies.

## A Brief Overview of MVI

If you’re not yet familiar with this architectural approach, you’ve likely encountered the widely-used MVVM in Android development. In short, MVI can be seen as a streamlined version of MVVM that limits the flow of data from the ViewModel to the View to two streams: the state stream and the effects (or one-time events) stream. Conceptually, there’s only one stream, but Android’s lifecycle nuances introduce the need for these two distinct flows.

• **State Stream**: Consolidates all `LiveData` instances in MVVM into a single flow that represents the entire screen state.

• **Effect Stream**: Groups one-time events like `SingleLiveEvent` in MVVM for transient actions.

In MVI, the View subscribes to the state stream and receives a complete data model at each update, unlike MVVM, where data updates are often fragmented across multiple `LiveData` instances. This ensures that the View always has a complete and consistent picture of the screen’s state. The initial state is also defined in the data model, avoiding scenarios where initial states are scattered across XML files or View settings, a common issue with MVVM.

```kotlin
class MVVMViewModel {
    val isProgress: LiveData<Boolean>
    val value: LiveData<Int>
}

// vs

data class State(val isProgress: Boolean, val value: Int)  

class MVIViewModel {
    val stateFlow: Flow<State> // A coroutine-based data stream, similar to Observable/Flowable in RxJava.
}
```
  
With this approach, there’s no need for the View to coordinate multiple, unrelated updates from the ViewModel or for the ViewModel to ensure all `LiveData` properties are updated. I find this to be a much cleaner and more intuitive solution.

### Core Components of MVI

• **Model**: A comprehensive data model (often a data class) that holds all the state required by the View. Unlike MVVM, where data may be scattered, MVI emphasizes consolidating public state into a single source of truth.
_Note_: While **sealed classes** can be also used, avoid turning them into “commands” for view, which can undermine the essence of MVI. I like how this is described in [Ballast library docs](https://copper-leaf.github.io/ballast/wiki/usage/mental-model/#kinds-of-state-classes)

• **View**: The UI component that consumes data and updates the interface. It receives complete state updates and must unconditionally synchronize itself to match the provided state.

• **Intent**: Represents UI events that modify the Model. In Android, these often declared as public ViewModel methods called by the View, but can also be implemented as data classes, making them easy to log and track.

• **ViewModel**: Although not explicitly named in MVI, it plays a critical role in Android. It maintains the Model’s lifecycle, handles configuration changes, and acts as an intermediary between the UI and other system components like databases or network layers.

### Key Properties of MVI

1. **Reactivity**: MVI embraces a reactive paradigm, ensuring that data flows smoothly between components.

2. **Single State**: The entire screen state is bundled into a single object, which eliminates fragmentation and simplifies updates.

3. **Unidirectional Data Flow (UDF)**: While the term “unidirectional” might be misleading, the key idea is maintaining a structured cycle where data flows between the ViewModel and View without chaotic interdependencies seen in architectures like MVP.

### Beyond the Basics

Some MVI implementations like [Badoo Core MVI](https://badoo.github.io/MVICore/features/reducerfeature/) introduce tools like reducers and actors, which can add complexity but may be helpful in large-scale applications. While some of these concepts originate from web development, they may not be necessary for simpler use cases like the one we’re exploring here.

For a minimalistic MVI implementation, I recommend checking out this repository: [GitHub - Orbit MVI](https://github.com/orbit-mvi/orbit-mvi)

## A Brief Overview of  State Machine

State Machine is an effective way to represent complex states consisting of multiple variables. Unlike a simple data class, a state machine not only describes the current state but also defines and **limits** the actions that can be performed in a specific state.

### Key Components of a State Machine

1. **Machine (StateMachine)**: A finite (predefined) set of states and transitions modeled for specific logic.

2. **State**: The individual states within the machine.

3. **Transition**: The movement of the machine from one state to another.

4. **Event**: An occurrence that triggers a transition between states.

### How StateMachine Works in Practice

Most libraries implementing this model, including the [KStateMachine](https://github.com/nsk90/kstatemachine), follow these steps:

1. **Define the Machine**:
   • Declare all possible states and transitions within the StateMachine.

2. **Run the Machine**:`
   • Start the StateMachine and send events to it.
   • The machine autonomously manages its current state, executing the necessary transitions.
   • Observe state transitions to trigger side effects or application-specific actions.

### Why Use StateMachine?

Using a StateMachine helps build a model that reacts to incoming events in a predictable and well-defined manner. By observing changes in this model, you can:

• Easily determine and act upon the current state of your application.
• Enforce valid state transitions, avoiding inconsistent or unexpected behaviors.
• Simplify complex logic by centralizing state and transitions.

This approach is particularly useful in applications with intricate workflows or business logic, where state management can otherwise become error-prone and hard to maintain.

## StateMachine Library vs Flags and `if-else` Statements

It’s tempting to use flags and `if-else` conditions to manage state transitions. However, as the number of flags and conditions grows, this approach becomes a maintenance nightmare. Code becomes fragile, difficult to read, and risky to modify.

The root problem is **freedom**: without constraints, developers can accidentally create invalid states or miss resetting flags. StateMachine, on the other hand, introduces rules and restrictions, enforcing valid transitions and making the system more predictable.

### Why Not Use `switch/when`?

For simple scenarios, you can implement a StateMachine using `switch/when`, like this:
  
```kotlin
fun nextState(currentState: State, event: Event): State {
    return when (currentState) {
        SomeState1 -> if (event == SomeEvent) NewState else currentState
        SomeState2 -> if (event == AnotherEvent) AnotherState else currentState
        // Additional cases...
    }
}
```
  
This approach works well when states are independent and transitions are straightforward. It’s certainly an improvement over ad hoc flag-based logic.

### Why `switch/when` Falls Short

The real challenges begin when states are **parallel** or **nested**:
1. **[Parallel States](https://kstatemachine.github.io/kstatemachine/pages/states/states.html#parallel-states)** allow multiple states to be active simultaneously. For example, a kettle can be:
   • Hot or cold.
   • Heating or cooling.
   
   Using a flat StateMachine to model this creates an explosion of state combinations, making the `switch/when` structure unwieldy.

2. **[Nested States](https://kstatemachine.github.io/kstatemachine/pages/states/states.html#nested-states)** introduce dependencies or hierarchy between states. For instance, a TV can be:
   • **On**: showing YouTube or a TV channel.
   • **Off**: where the state of the content being displayed becomes irrelevant.
   Managing such dependencies manually in `switch/when` logic results in convoluted and fragile code.

### Why Use a Library?

StateMachine libraries are designed to handle these scenarios elegantly by:
• Simplifying the modeling of parallel and nested states.
• Providing a robust framework for defining transitions and side effects.
• Ensuring invalid states or transitions are impossible by design.

For a deeper exploration of this topic, check out [David Khourshid’s article](https://dev.to/davidkpiano/you-don-t-need-a-library-for-state-machines-k7h), which discusses when you need a library for state machines.

## Game Sample Application

Let’s explore an example by modeling the behavior of a game shooter hero. This example highlights how StateMachine simplifies state management.

### Hero States
  
Our hero can be in the following conditions:
• **Shooting**
• **Reloading**
• **Standing**
• **Crouching**
• **Jumping**
• **Kicking mid-air**
 
These conditions represent the states of the hero in the game. Managing these states with boolean flags (e.g., `isStanding`, `isShooting`) might seem straightforward initially, but it introduces a high risk of errors. For instance:

• The hero might be both standing and crouching simultaneously due to incorrect flag handling.
• The hero might continue shooting even when out of ammo.

Using a StateMachine solves these problems by defining valid states and transitions, ensuring that only logical state combinations can occur.

### Benefits of StateMachine

1. **State Consistency**: A StateMachine eliminates the possibility of contradictory or invalid states. For example, if the hero is jumping, the StateMachine won’t allow them to transition directly to crouching without landing first.
2. **Simplified Logic**: State management is centralized, making the codebase easier to read and maintain.
3. **Scalability**: As the game grows in complexity (e.g., adding power-ups or new abilities), the StateMachine can adapt without introducing bugs or breaking existing logic.
  
## Code Example

For a hands-on example, check out the complete implementation on GitHub: [Android KStateMachine Sample](https://github.com/nsk90/android-kstatemachine-sample)
There is also similar [Compose sample]((https://github.com/KStateMachine/compose-kstatemachine-sample)) if you are familiar with this technology.

### View/ViewModel (MVI)  

To begin, let’s outline how the **View** and **ViewModel** interact using the MVI architecture. We’ll stick to the essentials for clarity.

If you’d like to dive deeper into the MVI implementation (only ~40 lines of code), you can find it here: [MVI on GitHub](https://github.com/nsk90/android-kstatemachine-sample/blob/main/app/src/main/java/ru/nsk/kstatemachinesample/mvi/Mvi.kt).

### Key Components

```kotlin
data class ModelData(val ammoLeft: UInt, val activeStates: List<HeroState>)  

sealed interface ModelEffect {
    object AmmoDecremented : ModelEffect
    class StateEntered(val state: HeroState) : ModelEffect
    class ControlEventSent(val event: ControlEvent) : ModelEffect
}

class MainViewModel : MviModelHost<ModelData, ModelEffect>, ViewModel() {
    override val model = model(viewModelScope, ModelData(INITIAL_AMMO, listOf(Standing)))
    // ...
}
```
  
• `ModelData`: Represents the MVI state, including the hero’s active states (`List<HeroState>`).
• `ModelEffect`: Defines one-off effects, like logging or sending control events.

The `MainViewModel` combines `androidx.ViewModel` and `MviModelHost`, initializing the framework with model, which requires an initial state (`ModelData`).

### Connecting the View

The **View** subscribes to `ModelData` and `ModelEffect` streams upon creation, reacting to state updates and effects.

```kotlin
class MainFragment : Fragment() {
    private val viewModel by viewModel<MainViewModel>()

    override fun onViewCreated(...) {
        jumpButton.onClick { viewModel.sendEvent(JumpPressEvent) } // Sending MVI intents (events)
        viewModel.observe(this, ::onStateChanged, ::onEffect)
    }

    private fun onStateChanged(state: ModelData) {
        // Update UI visuals
    }

    private fun onEffect(effect: ModelEffect) {
        // Handle effects (e.g., logging, toasts)
    }
}
```
  
**Simplified UI Handling**
1. **ModelData Updates**:
   • The View renders hero states by changing pre-drawn images based on the current `ModelData`.
   • This ensures the View always reflects the latest state accurately.

2. **ModelEffect Usage**:
   • While effects like logging are shown here, in real-world applications, they handle transient actions like navigation, displaying toasts, or requesting permissions.

3. **Intent Dispatch**:
   • Button clicks in the **View** send `ControlEvents` to the ViewModel, serving as MVI intents.

## Modeling the Hero (StateMachine)

Now let's move to the ViewModel side, where we will model the hero's actions using the StateMachine.

### States

To define the hero's states, we use a `sealed class`. In **KStateMachine**, one way to create a state is by inheriting from the `DefaultState` class.

```kotlin
sealed class HeroState : DefaultState() {
    object Standing : HeroState()
    object Jumping : HeroState()
    object Ducking : HeroState()
    class AirAttacking : HeroState() {
        var isDownPressed = true  // Flag indicating whether the Down button is pressed during air attack
    }
    object NotShooting : HeroState()
    class Shooting : HeroState() {
        lateinit var shootingTimer: Job  // Timer to manage the shooting action
    }
}
```

Some states contain data specific to them, such as the `Job` in the `Shooting` state, which controls the shooting timer. We’ll revisit these data points later.  

It’s important to note that the hero can perform multiple actions at once, like shooting while standing or jumping. If we tried to describe every possible combination of actions, the number of possible state combinations would quickly become too large and hard to manage. To avoid this, we will use the concept of **parallel states**, which allows multiple actions to occur simultaneously. You can read more about parallel states in the [KStateMachine documentation](https://kstatemachine.github.io/kstatemachine/pages/states/states.html#parallel-states).

### Events

We also need events from the outside world (like screen touches) to interact with the hero. In this example, the events are simple and don’t contain any data, unlike in real-world applications where events often carry data. These events simply represent the buttons pressed by the player.

In the StateMachine concept, it’s important to treat events strictly as **external triggers** (e.g., button presses) rather than as commands. The state machine itself is the **source of truth**, responsible for managing the current state. Events are provided to the state machine to inform it of changes in the environment. When events are treated as commands and directly force state transitions, issues arise. The decision-making process for transitioning to the next state starts to leak outside the state machine, compromising the integrity and encapsulation of the system.

While the distinction between an event and a command can sometimes be subtle, it's important to ensure that decisions about state transitions are made within the state machine, not from external sources.

```kotlin
sealed interface ControlEvent : Event {
    object JumpPressEvent : ControlEvent
    object JumpCompleteEvent : ControlEvent
    object DuckPressEvent : ControlEvent
    object DuckReleaseEvent : ControlEvent
    object FirePressEvent : ControlEvent
    object FireReleaseEvent : ControlEvent
}
private object OutOfAmmoEvent : ControlEvent
```

The `OutOfAmmoEvent` is marked as private because it’s an event that the View should not send directly. Instead, the ViewModel generates this event internally based on the remaining ammo count.

### StateMachine

Now that we’ve outlined the individual states and events, it’s time to integrate everything and define the state transitions.

We declare the StateMachine using one of **KStateMachine**'s specialized functions:

```kotlin
createStateMachineBlocking(viewModelScope, "Hero", ChildMode.PARALLEL) {
    state("Movement") {
        // ...
    }
    state("Fire") {
        // ...
    }
}
```

In KStateMachine, the StateMachine itself is the parent state, and we specify that child states will be “parallel” by passing `ChildMode.PARALLEL`. This structure helps us organize the states into two primary groups: those related to movement (`Movement`) and those for shooting (`Fire`).

### Fire State Example

Let’s start with the shooting state. Since there are only two possible states (shooting and not shooting), this will be easier to explain:

```kotlin
state("Fire") {
    val shooting = addState(Shooting()) // add the Shooting state

    addInitialState(NotShooting) { // add and set the initial state to NotShooting
        transition<FirePressEvent> { // define a transition to the Shooting state on a "fire" button press event
            guard = { state.ammoLeft > 0u } // cannot start shooting if there is no ammo
            targetState = shooting
        }
    }
    shooting { // configure the Shooting state
        transition<FireReleaseEvent>(targetState = NotShooting) // transition to NotShooting when the fire button is released
        transition<OutOfAmmoEvent>(targetState = NotShooting) // similarly, transition to NotShooting when ammo runs out

        onEntry { // upon entering the state, start a timer that will fire bullets at a set interval
            shootingTimer = viewModelScope.launch {
                tickerFlow(SHOOTING_INTERVAL_MS).collect {
                    if (state.ammoLeft == 0u)
                        sendEvent(OutOfAmmoEvent) // when ammo runs out, send the OutOfAmmoEvent
                    else
                        decrementAmmo() // decrement the ammo counter
                }
            }
        }
        onExit { shootingTimer.cancel() } // clean up the timer resource when exiting the state
    }
}
```
  
You can notice that in the decrementAmmo() function, two similar events are sent from the ViewModel to the View: a new state with a reduced ammo count and the AmmoDecremented effect. In this case, relying solely on the state is not sufficient because these events are logged, and logging must not be repeated during configuration changes or when the View re-subscribes to the ViewModel.

Another important feature to highlight is the guard field in the transition declaration. It allows you to define conditional transitions based on your application’s data. This kind of dynamically determined behavior in the StateMachine is incredibly useful and often essential in real-world applications.

Returning to the fields of State, here we use shootingTimer, which belongs to the Shooting state. In KStateMachine, states are not mere constants (like enum elements, as often seen in minimal examples); they are full-fledged objects capable of encapsulating and carrying associated data and resources.

### Movement State Example

Next, we model the movement states, such as standing, jumping, and ducking. We’ll also introduce the `AirAttacking` state, which occurs when the player is in mid-air while pressing the duck button.
  
```kotlin
state("Movement") {
    val airAttacking = addState(AirAttacking())

    addInitialState(Standing) {
        transition<JumpPressEvent>("Jump", targetState = Jumping)
        transition<DuckPressEvent>("Duck", targetState = Ducking)
    }

    addState(Jumping) {
        onEntry {
            viewModelScope.singleShotTimer(JUMP_DURATION_MS) { // jump timer (similar to gravity)
                sendEvent(JumpCompleteEvent)
            }
        }
        transition<DuckPressEvent>("AirAttack", targetState = airAttacking)
        transition<JumpCompleteEvent>("Land after jump", targetState = Standing) // when a regular jump ends, transition to Standing
    }

    addState(Ducking) {
        transition<DuckReleaseEvent>("StandUp", targetState = Standing)
    }

    airAttacking {
        onEntry { isDuckPressed = true } // instead of a flag, nested states could also be used (added here for variety)

        transitionOn<JumpCompleteEvent>("Land after attack") { // if "duck" is still pressed, the hero lands in a crouched state
            targetState = { if (this@airAttacking.isDuckPressed) Ducking else Standing } 
        }
        transition<DuckPressEvent>("Duck pressed") {
            onTriggered { this@airAttacking.isDuckPressed = true }
        }
        transition<DuckReleaseEvent>("Duck released") {
            onTriggered { this@airAttacking.isDuckPressed = false }
        }
    }
}
```

In the `AirAttacking` state, `isDuckPressed` helps track whether the duck button is held. Ideally, we could replace this flag with nested states or separate state machine, which would be beneficial for larger applications where managing multiple button states is common.

### Conclusion
With the configuration complete, it’s time to launch the game and see how the “gameplay” has turned out!

## **Summary**

Although MVI and StateMachine serve different purposes, have distinct goals, and gained popularity at different times in the industry, the ideas underlying both approaches share many similarities. These include reactivity, event-driven design, and state encapsulation. These common traits allow MVI and StateMachine to complement each other effectively, leveraging their respective strengths. Both can function independently or together, providing flexibility and maximum control over application behavior. When MVI alone struggles with the complexity of state management, it can be enhanced with a StateMachine, resulting in a more maintainable and user-friendly solution.

In the MVI implementation discussed, there was no reducer function (commonly used to derive new states from processing intents). However, some MVI frameworks include this feature, effectively embedding a `switch/when`-style StateMachine directly within the MVI architecture. As we’ve observed, this type of StateMachine is limited in functionality and works well only up to a certain level of complexity. I would consider this a drawback of rigid MVI frameworks. 

That’s all from me for now—thank you for reading to the end!

Please share your experiences with combining MVI and StateMachine in the comments. I’d love to hear which frameworks are most popular and how well they integrate with each other!