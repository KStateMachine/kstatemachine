# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build                        # build all modules
./gradlew :tests:jvmTest               # run all tests (JVM target)
./gradlew :tests:jvmTest --tests "ru.nsk.kstatemachine.statemachine.StateMachineTest" # run a single test class
./gradlew apiDump                      # regenerate .api files after public API changes
./gradlew apiCheck                     # validate public API matches committed .api files (runs in build)
./gradlew koverReport                  # generate code coverage report (min 88%)
```

## Module Structure

Five Gradle subprojects:

- **`kstatemachine`** — core library. Zero dependencies: no Kotlin Coroutines, no Android SDK. Uses stdlib coroutine primitives only via `StdLibCoroutineAbstraction`. Entry point: `createStdLibStateMachine { }`.
- **`kstatemachine-coroutines`** — adds full Kotlin Coroutines support. Entry point: `createStateMachine(scope) { }` (suspend). Provides `processEventByLaunch` / `processEventByAsync` and `StateMachineFlow` for `Flow`-based observation.
- **`kstatemachine-serialization`** — adds `kotlinx.serialization` support for persisting `RecordedEvents` via `RecordedEventsSerializer` and `SavedStateConfig` via `SavedStateConfigSerializer`.
- **`tests`** — all integration/unit tests. Depends on all three library modules. Uses Kotest + MockK. Only JVM target is run locally (other targets require platform toolchains).
- **`samples`** — standalone usage examples, excluded from API validation.

All library versions live in `buildSrc/src/main/kotlin/Versions.kt`.

## Architecture

### Coroutine abstraction layer

`CoroutineAbstraction` (in `kstatemachine`) is the key seam that lets the core library expose `suspend` functions without depending on `kotlinx.coroutines`. `StdLibCoroutineAbstraction` drives coroutines via raw `Continuation` from the stdlib; `CoroutinesLibCoroutineAbstraction` (in `kstatemachine-coroutines`) wraps a `CoroutineScope`.

**Important thread-safety constraint**: `StateMachine` is not thread-safe. Always use a single-threaded `CoroutineScope`. The library enforces this at construction time and rejects `Dispatchers.Default` / `Dispatchers.IO` unless `skipCoroutineScopeValidityCheck` is set.

### State type hierarchy

```
IState  ──►  State          (plain state, no data)
        ──►  DataState<D>   (holds typed data while active)
               ──►  MutableDataState<D>  (data settable manually)
        ──►  IFinalState    (machine stops on entry)
        ──►  PseudoState    (machine passes through automatically)
               ──►  HistoryState / RedirectPseudoState
StateMachine  extends  State   (a machine is itself a state → composable)
```

`ChildMode.EXCLUSIVE` (default) = one active child at a time; `ChildMode.PARALLEL` = all children active simultaneously.

### Event processing

Events are processed synchronously within a single-threaded context. While an event is being processed, additional events are queued and dispatched to `PendingEventHandler`. `processEvent()` returns `ProcessingResult` (PROCESSED / IGNORED / PENDING).

### Visitor pattern

`CoVisitor` (suspendable) and `Visitor` (sync) walk the machine tree: `StateMachine → IState → Transition`. Used internally for export (PlantUML/Mermaid), structure hashing, active-state collection, and cleanup. Prefer adding new cross-cutting traversals as visitors rather than recursive extension functions.

### Persistence

`EventRecorder` captures every processed event into `RecordedEvents` (enabled via `CreationArguments.eventRecordingArguments`). To restore state: replay `RecordedEvents` on a freshly constructed identical machine via `restoreByRecordedEvents()`. The `structureHashCode` guards against replaying on a structurally different machine. `kstatemachine-serialization` provides a `RecordedEventsSerializer` for JSON persistence.

### Public API compatibility

The `binary-compatibility-validator` plugin tracks public API in `<module>/api/<module>.api` files. After any public API change run `./gradlew apiDump` to update these files and commit them alongside the code change. `apiCheck` runs as part of `build` and will fail if the files are out of date.

### Testing utilities

`Testing` object (imported via `import ru.nsk.kstatemachine.testing.Testing.*`) provides `startFrom(state)` / `startFromBlocking(state)` to start the machine from a specific state, bypassing normal initialization. The `@VisibleForTesting` annotation marks internal API exposed only for tests; such members are excluded from public API validation.
