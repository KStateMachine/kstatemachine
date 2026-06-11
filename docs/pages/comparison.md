---
layout: page
title: Library Comparison
---

# Library Comparison
{: .no_toc }

## Page contents
{: .no_toc .text-delta }

- TOC
{:toc}

This page compares KStateMachine with other popular state machine libraries.

Feature list changes over time — check the official repository of each library for the most current information.

## Libraries

| Library                  | Language | Repository                                                                                               |
|--------------------------|----------|----------------------------------------------------------------------------------------------------------|
| **KStateMachine**        | Kotlin   | [github.com/KStateMachine/kstatemachine](https://github.com/KStateMachine/kstatemachine)                 |
| **FlowRedux**            | Kotlin   | [github.com/freeletics/FlowRedux](https://github.com/freeletics/FlowRedux)                               |
| **Tinder StateMachine**  | Kotlin   | [github.com/Tinder/StateMachine](https://github.com/Tinder/StateMachine)                                 |
| **Spring State Machine** | Java     | [github.com/spring-projects/spring-statemachine](https://github.com/spring-projects/spring-statemachine) |

## Feature matrix (explored by AI)

| Feature                                  | KStateMachine | FlowRedux  | Tinder StateMachine |  Spring State Machine  |
|------------------------------------------|:-------------:|:----------:|:-------------------:|:----------------------:|
| **Language**                             |    Kotlin     |   Kotlin   |       Kotlin        |          Java          |
| **Active maintenance**                   |       ✅       |     ✅      |          ✅          |           ✅            |
| **Kotlin Multiplatform**                 |       ✅       |     ✅      |          ❌          |           ❌            |
| **Kotlin Coroutines**                    |       ✅       |     ✅      |          ❌          |           ❌            |
| **Zero mandatory dependencies**          |       ✅       |     ❌      |          ❌          |           ❌            |
| **API style**                            |  Kotlin DSL   | Kotlin DSL |     Kotlin DSL      | Builders + annotations |
| **Hierarchical states (HSM)**            |       ✅       |     ❌      |          ❌          |           ✅            |
| **Parallel states (orthogonal regions)** |       ✅       |     ❌      |          ❌          |           ✅            |
| **History states (shallow + deep)**      |       ✅       |     ❌      |          ❌          |           ✅            |
| **Pseudo states (choice, fork/join)**    |       ✅       |     ❌      |          ❌          |           ✅            |
| **Typesafe / data transitions**          |       ✅       |     ✅      |          ✅          |           ⚠️           |
| **Transition inheritance**               |       ✅       |     ❌      |          ❌          |           ✅            |
| **Undo transitions**                     |       ✅       |     ❌      |          ❌          |           ❌            |
| **Export (PlantUML / Mermaid)**          |       ✅       |     ❌      |          ❌          |           ❌            |
| **IntelliJ IDE plugin**                  |       ✅       |     ❌      |         ⚠️          |           ❌            |
| **Persistence / serialization**          |       ✅       |     ❌      |          ❌          |           ✅            |
| **Testing helpers**                      |       ✅       |     ❌      |          ❌          |           ✅            |

⚠️ — partial support; see library documentation for details.

## Architectural note

**FlowRedux** takes a Redux-inspired approach: states are flat sealed interfaces. It is
good for reactive UIs driven by Kotlin Coroutines `Flow`, but it does not implement
the UML statechart model (no hierarchy, parallel regions, or pseudo states).

**Tinder StateMachine** is a minimal library — a flat FSM with sealed-class type
safety. Suitable for simple use-cases where a full statechart is not required.