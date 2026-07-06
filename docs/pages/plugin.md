---
layout: page
title: IntelliJ IDEA Plugin
nav_order: 5
---

# KStateMachine Visual — IntelliJ Plugin
{: .no_toc }

## Page contents
{: .no_toc .text-delta }

- TOC
{:toc}

**KStateMachine Visual** is an IntelliJ IDEA plugin that visualizes and navigates your state machines
directly from Kotlin source — no runtime required.

Unlike runtime export-based diagram generation, the plugin uses **static analysis** of your Kotlin code,
so it finds every state and transition — including those inside conditional branches that runtime
export cannot reach.

[![JetBrains Marketplace](https://img.shields.io/jetbrains/plugin/v/32202-kstatemachine-visual?label=JetBrains%20Marketplace&logo=jetbrains)](https://plugins.jetbrains.com/plugin/32202-kstatemachine-visual/)

## Install

**Via Marketplace (recommended)**

`Settings → Plugins → Marketplace` → search **KStateMachine Visual** → Install

**Manual install**

Download the latest `.zip` from the
[plugin releases](https://github.com/KStateMachine/kstatemachine-intellij-platform-plugin/releases)
and use `Settings → Plugins → ⚙ → Install Plugin from Disk…`

**Requirements:** IntelliJ Platform 2026.1 or later with the bundled Kotlin and Java plugins.

## Features

### Tree view

Every state machine in the open project is shown as a navigable tree in a dedicated tool window.
Specialised icons distinguish state types at a glance:

| Icon type | State kind                       |
|-----------|----------------------------------|
| Initial   | Initial state / entry point      |
| Final     | Final state (machine stops here) |
| Choice    | Choice pseudo-state              |
| History   | History pseudo-state             |
| Data      | `DataState` / `DataFinalState`   |
| Parallel  | Parallel region container        |

### Live PlantUML diagram

The active state machine is rendered as a UML statechart using JS layout engine —
**no Graphviz installation required**. The diagram updates in real time as you edit the source,
with debounced refresh to avoid interrupting typing.

### Bidirectional navigation

- Click any node in the tree to jump to the corresponding declaration in the editor.
- Place the cursor on a KStateMachine DSL call — the tree scrolls to that node automatically.

### Gutter icons

A small icon appears in the editor gutter next to every KStateMachine DSL declaration
(`createStateMachine`, `initialState`, `state`, `transition`, etc.), making it easy to spot
machine code at a glance and navigate directly to the tree view.

### Export

From the diagram panel you can:

- **Copy PlantUML source** to the clipboard for use in documentation or external tools.
- **Export as PNG or SVG** for sharing or embedding in wikis.

### PlantUML Playground

A dedicated **Playground** tab lets you write and preview ad-hoc PlantUML code without leaving
the IDE — handy for experimenting with diagram styles or manually sketching a design before coding it.

### Recursive DSL parsing

The plugin resolves the full DSL tree at any nesting depth, including states declared inside
conditional branches (`if`, `when`, `transitionConditionally`, etc.) that runtime export skips
because they are not reachable without running the code.

## Comparison with runtime export

|                                                 | IntelliJ Plugin              | Runtime export (`exportToPlantUml/Mermaid`)        |
|-------------------------------------------------|------------------------------|----------------------------------------------------|
| Requires running the app                        | No                           | Yes                                                |
| Sees conditional branches                       | Yes                          | Limited (with `unsafeCallConditionalLambdas` flag) |
| Real-time diagram updates                       | Yes                          | No (Brawser is required)                           |
| IDE navigation                                  | Yes                          | No                                                 |
| Immune to the syntax changes, and code location | No                           | Yes                                                |
| Output format                                   | SVG, PlantUML / Mermaid text | PlantUML / Mermaid text                            |

Both approaches complement each other: use the plugin for development-time exploration
and navigation; use runtime export when you need diagrams embedded in documentation or CI artefacts.

## Source & issues

Plugin source code:
[github.com/KStateMachine/kstatemachine-intellij-platform-plugin](https://github.com/KStateMachine/kstatemachine-intellij-platform-plugin)

Report bugs or request features in the plugin repository's
[Issues](https://github.com/KStateMachine/kstatemachine-intellij-platform-plugin/issues) tracker.
