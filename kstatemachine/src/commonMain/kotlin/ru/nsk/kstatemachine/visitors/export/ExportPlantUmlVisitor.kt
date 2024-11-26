/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.visitors.export

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.isNeighbor
import ru.nsk.kstatemachine.metainfo.UmlMetaInfo
import ru.nsk.kstatemachine.metainfo.MetaInfo
import ru.nsk.kstatemachine.metainfo.findMetaInfo
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.state.pseudo.UndoState
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.transition.EventAndArgument
import ru.nsk.kstatemachine.transition.InternalTransition
import ru.nsk.kstatemachine.transition.Transition
import ru.nsk.kstatemachine.transition.TransitionDirectionProducerPolicy
import ru.nsk.kstatemachine.transition.TransitionDirectionProducerPolicy.CollectTargetStatesPolicy
import ru.nsk.kstatemachine.transition.TransitionDirectionProducerPolicy.UnsafeCollectTargetStatesPolicy
import ru.nsk.kstatemachine.visitors.CoVisitor
import ru.nsk.kstatemachine.visitors.export.CompatibilityFormat.MERMAID
import ru.nsk.kstatemachine.visitors.export.CompatibilityFormat.PLANT_UML

private const val STAR = "[*]"
private const val SINGLE_INDENT = "    "
private const val PARALLEL = "--"
private const val SHALLOW_HISTORY = "[H]"
private const val DEEP_HISTORY = "[H*]"
private const val CHOICE = "<<choice>>"

/**
 * This object will be unsafely cast to any kind of [Event],
 * causing runtime failures if user defined (conditional) lambdas will touch this object.
 */
internal object ExportPlantUmlEvent : Event

internal enum class CompatibilityFormat { PLANT_UML, MERMAID }

/**
 * Export state machine to Plant UML language format.
 * @see <a href="https://plantuml.com/ru/state-diagram">Plant UML state diagram</a>
 *
 * Conditional transitions are partly supported with [unsafeCallConditionalLambdas] flag.
 * Uses [UmlMetaInfo] for output sugaring.
 */
internal class ExportPlantUmlVisitor(
    private val format: CompatibilityFormat,
    private val showEventLabels: Boolean,
    private val unsafeCallConditionalLambdas: Boolean,
) : CoVisitor {
    private val builder = StringBuilder()
    private var indent = 0
    private val crossLevelTransitions = mutableListOf<String>()

    fun export() = builder.toString()

    override suspend fun visit(machine: StateMachine) {
        when (format) {
            PLANT_UML -> {
                line("@startuml")
                line("hide empty description")
            }
            MERMAID -> line("stateDiagram-v2")
        }

        line("state ${machine.labelGraphName()} {")
        ++indent
        processStateBody(machine)
        --indent
        line("}")

        crossLevelTransitions.forEach { line(it) }

        if (format == PLANT_UML)
            line("@enduml")
    }

    override suspend fun visit(state: IState) {
        if (state.states.isEmpty()) {
            when (state) {
                is HistoryState, is UndoState -> return
                is RedirectPseudoState -> {
                    line("state ${state.labelGraphName()} $CHOICE")
                    @Suppress("UNCHECKED_CAST")
                    val targetStates = state.resolveTargetState(makeDirectionProducerPolicy<Event>())
                        .targetStates as Set<InternalState>
                    state.printStateNotes()
                    targetStates.forEach { targetState ->
                        crossLevelTransitions += "${state.graphName()} --> ${targetState.targetGraphName()}"
                    }
                }
                else -> {
                    line("state ${state.labelGraphName()}")
                    state.printStateDescriptions()
                    state.printStateNotes()
                }
            }
        } else {
            if (state !is StateMachine) { // ignore composed machines
                line("state ${state.labelGraphName()} {")
                ++indent
                processStateBody(state)
                --indent
                line("}")
            } else {
                line("state ${state.labelGraphName()}")
            }
        }
    }

    /**
     * PlantUML cannot show correctly cross-level transitions to nested states.
     * It requires to see all states declarations first to provide correct rendering,
     * so we have to store them to print after state declaration.
     */
    override suspend fun <E : Event> visit(transition: Transition<E>) {
        transition as InternalTransition<E>

        val sourceState = transition.sourceState.graphName()

        @Suppress("UNCHECKED_CAST")
        val targetStates = transition.produceTargetStateDirection(makeDirectionProducerPolicy()).targetStates
                as Set<InternalState>
        targetStates.forEach { targetState -> // actually plantUml may not understand multiple transitions
            val transitionString = "$sourceState --> ${targetState.targetGraphName()}${transitionLabel(transition)}"

            if (transition.sourceState.isNeighbor(targetState))
                line(transitionString)
            else
                crossLevelTransitions += transitionString
        }

        if (format != MERMAID) { // Mermaid does not support this
            transition.metaInfo?.umlNotes?.forEach {
                line("note on link")
                line("$SINGLE_INDENT$it")
                line("end note")
            }
        }
    }

    private fun <E : Event> makeDirectionProducerPolicy(): TransitionDirectionProducerPolicy<E> {
        return if (unsafeCallConditionalLambdas) {
            @Suppress("UNCHECKED_CAST") // this is unsafe by design
            UnsafeCollectTargetStatesPolicy(EventAndArgument(ExportPlantUmlEvent as E, null))
        } else {
            CollectTargetStatesPolicy()
        }
    }

    private suspend fun processStateBody(state: IState) {
        state.printStateDescriptions()
        state.printStateNotes()

        val states = state.states.toList()
        // visit child states
        for (s in states.indices) {
            visit(states[s])
            if (s != states.lastIndex && state.childMode == ChildMode.PARALLEL)
                line(PARALLEL)
        }

        // add initial transition
        line("")
        val initialState = state.initialState
        if (initialState != null)
            line("$STAR --> ${initialState.graphName()}")

        // visit transitions
        states.flatMap { it.transitions }.forEach { visit(it) }

        // add finish transitions
        states.filterIsInstance<IFinalState>()
            .forEach { line("${it.graphName()} --> $STAR") }
    }

    private fun line(text: String) = builder.appendLine(SINGLE_INDENT.repeat(indent) + text)

    private fun transitionLabel(transition: Transition<*>): String {
        val text = listOfNotNull(
            transition.metaInfo?.umlLabel ?: transition.name,
            transition.eventMatcher.eventClass.simpleName.takeIf { showEventLabels },
        ).joinToString()
        return " : $text".takeIf { text.isNotBlank() } ?: ""
    }

    private fun IState.printStateDescriptions() {
        val descriptions = (metaInfo as? UmlMetaInfo)?.umlStateDescriptions.orEmpty()
        descriptions.forEach { line("${graphName()} : $it") }
    }

    private fun IState.printStateNotes() {
        metaInfo?.umlNotes?.forEach { line("note right of ${graphName()} : $it") }
    }

    private companion object {
        val MetaInfo.umlNotes get() = findMetaInfo<UmlMetaInfo>()?.umlNotes.orEmpty()
        val MetaInfo.umlLabel get() = findMetaInfo<UmlMetaInfo>()?.umlLabel

        fun IState.graphName(): String {
            val name = (name ?: "State${hashCode()}").replace(Regex("[ -]"), "_")
            return if (this !is StateMachine) name else "${name}_StateMachine"
        }

        fun IState.labelGraphName(): String {
            // Mermaid does not support - state name as "long name", notation
            return metaInfo?.umlLabel?.let { "\"$it\" as " }.orEmpty() + graphName()
        }

        fun InternalState.targetGraphName(): String {
            return if (this is HistoryState) {
                val prefix = requireInternalParent().graphName()
                when (historyType) {
                    HistoryType.SHALLOW -> "$prefix$SHALLOW_HISTORY"
                    HistoryType.DEEP -> "$prefix$DEEP_HISTORY"
                }
            } else {
                graphName()
            }
        }
    }
}

/**
 * Export [StateMachine] to PlantUML state diagram
 * @see <a href="https://plantuml.com/">PlantUML</a>
 *
 * [showEventLabels] prints event types for transitions
 * [unsafeCallConditionalLambdas] will call conditional lambdas which can touch application data,
 * this may give more complete output, but may be not safe.
 */
suspend fun StateMachine.exportToPlantUml(
    showEventLabels: Boolean = false,
    unsafeCallConditionalLambdas: Boolean = false,
) = with(ExportPlantUmlVisitor(PLANT_UML, showEventLabels, unsafeCallConditionalLambdas)) {
    accept(this)
    export()
}

/** Blocking analog for [exportToPlantUml] */
fun StateMachine.exportToPlantUmlBlocking(
    showEventLabels: Boolean = false,
    unsafeCallConditionalLambdas: Boolean = false,
) = coroutineAbstraction.runBlocking {
    with(ExportPlantUmlVisitor(PLANT_UML, showEventLabels, unsafeCallConditionalLambdas)) {
        accept(this)
        export()
    }
}