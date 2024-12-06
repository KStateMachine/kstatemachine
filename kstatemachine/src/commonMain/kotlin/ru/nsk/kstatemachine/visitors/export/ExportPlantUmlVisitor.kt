/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.visitors.export

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.isNeighbor
import ru.nsk.kstatemachine.metainfo.EventAndArgumentResolutionHint
import ru.nsk.kstatemachine.metainfo.ExportMetaInfo
import ru.nsk.kstatemachine.metainfo.IgnoreUnsafeCallConditionalLambdasMetaInfo
import ru.nsk.kstatemachine.metainfo.UmlMetaInfo
import ru.nsk.kstatemachine.metainfo.MetaInfo
import ru.nsk.kstatemachine.metainfo.ResolutionHint
import ru.nsk.kstatemachine.metainfo.StateResolutionHint
import ru.nsk.kstatemachine.metainfo.findMetaInfo
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.state.pseudo.UndoState
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.transition.EventAndArgument
import ru.nsk.kstatemachine.transition.InternalTransition
import ru.nsk.kstatemachine.transition.Transition
import ru.nsk.kstatemachine.transition.TransitionDirection
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

private data class TargetStateInfo(
    val description: String?,
    val targetStates: Set<InternalState>,
) {
    init {
        require(targetStates.isNotEmpty()) { "targetStates must be non-empty." }
    }

    /**
     * PlantUML cant draw multiple target transitions.
     * So I have to simplify it to just use first state.
     */
    val targetState get() = targetStates.first()
}

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
                    val targetStateInfoList = executeDirectionProducerPolicy<Event>(state.metaInfo) { policy ->
                        state.resolveTargetState(policy)
                    }
                    state.printStateNotes()
                    targetStateInfoList.forEach { targetStateInfo ->
                        //todo use description
                        crossLevelTransitions += "${state.graphName()} --> ${targetStateInfo.targetState.targetGraphName()}"
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
        val targetStateInfoList = executeDirectionProducerPolicy<E>(transition.metaInfo) { policy ->
            transition.produceTargetStateDirection(policy)
        }
        targetStateInfoList.forEach { targetStateInfo -> // actually plantUml may not understand multiple transitions
            //todo use description
            val transitionString =
                "$sourceState --> ${targetStateInfo.targetState.targetGraphName()}${transitionLabel(transition)}"

            if (transition.sourceState.isNeighbor(targetStateInfo.targetState))
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

    /**
     * We should call conditional lambdas if [hasUnsafeCallConditionalLambdas] is true and use user provided
     * [ResolutionHint]s to extend output.
     */
    private suspend fun <E : Event> executeDirectionProducerPolicy(
        metaInfo: MetaInfo?,
        block: suspend (TransitionDirectionProducerPolicy<E>) -> TransitionDirection
    ): List<TargetStateInfo> {
        val stateInfoList = mutableListOf<TargetStateInfo>()
        if (metaInfo.hasUnsafeCallConditionalLambdas) {
            val exportMetaInfo = metaInfo.findMetaInfo<ExportMetaInfo>()
            if (exportMetaInfo != null) {
                val hintMap = exportMetaInfo.resolutionHints.groupBy {
                    when (it) {
                        is EventAndArgumentResolutionHint -> EventAndArgumentResolutionHint::class
                        is StateResolutionHint -> StateResolutionHint::class
                    }
                }
                if (hintMap.isNotEmpty()) {
                    hintMap[EventAndArgumentResolutionHint::class]?.mapTo(stateInfoList) {
                        it as EventAndArgumentResolutionHint
                        @Suppress("UNCHECKED_CAST")
                        val eventAndArgument = it.eventAndArgument as EventAndArgument<E>
                        TargetStateInfo(
                            it.description,
                            block(makeDirectionProducerPolicy<E>(metaInfo, eventAndArgument)).internalTargetStates
                        )
                    }
                    hintMap[StateResolutionHint::class]?.mapTo(stateInfoList) {
                        it as StateResolutionHint
                        TargetStateInfo(it.description, it.internalTargetStates)
                    }
                } else {
                    stateInfoList += TargetStateInfo(null, block(makeDirectionProducerPolicy<E>(metaInfo)).internalTargetStates)
                }
            } else {
                stateInfoList += TargetStateInfo(null, block(makeDirectionProducerPolicy<E>(metaInfo)).internalTargetStates)
            }
        } else {
            stateInfoList += TargetStateInfo(null, block(makeDirectionProducerPolicy<E>(metaInfo)).internalTargetStates)
        }
        return stateInfoList
    }

    private val MetaInfo?.hasUnsafeCallConditionalLambdas: Boolean
        get() = unsafeCallConditionalLambdas && findMetaInfo<IgnoreUnsafeCallConditionalLambdasMetaInfo>() == null

    private fun <E : Event> makeDirectionProducerPolicy(
        metaInfo: MetaInfo?,
        @Suppress("UNCHECKED_CAST") // this is unsafe by design
        eventAndArgument: EventAndArgument<E> = EventAndArgument(ExportPlantUmlEvent as E, null)
    ): TransitionDirectionProducerPolicy<E> {
        return if (metaInfo.hasUnsafeCallConditionalLambdas)
            UnsafeCollectTargetStatesPolicy(eventAndArgument)
        else
            CollectTargetStatesPolicy()
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

@Suppress("UNCHECKED_CAST")
private val TransitionDirection.internalTargetStates get() = targetStates as Set<InternalState>

/**
 * Export [StateMachine] to PlantUML state diagram
 * @see <a href="https://plantuml.com/">PlantUML</a>
 *
 * [showEventLabels] prints event types for transitions
 * [unsafeCallConditionalLambdas] will call conditional lambdas which can touch application data,
 * this may give more complete output, but may be not safe ([ClassCastException] may be thrown).
 *  * See [ExportMetaInfo] for more info.
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