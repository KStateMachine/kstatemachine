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
import ru.nsk.kstatemachine.statemachine.START_TRANSITION_NAME
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.UNDO_TRANSITION_NAME
import ru.nsk.kstatemachine.transition.EventAndArgument
import ru.nsk.kstatemachine.transition.InternalTransition
import ru.nsk.kstatemachine.transition.NoTransition
import ru.nsk.kstatemachine.transition.Stay
import ru.nsk.kstatemachine.transition.TargetState
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
    val transitionDirection: TransitionDirection,
)

private val internalTransitions = listOf(START_TRANSITION_NAME, UNDO_TRANSITION_NAME)

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
        machine.transitions.forEach {
            if (it.name !in internalTransitions) visit(it)
        }
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
                    val stateGraphName = state.graphName()
                    for (targetStateInfo in targetStateInfoList) {
                        val transitionDirection = targetStateInfo.transitionDirection
                        when (transitionDirection) {
                            NoTransition -> continue
                            // not possible actually, as it is an infinite loop
                            Stay -> crossLevelTransitions += "$stateGraphName --> $stateGraphName" +
                                    transitionLabel(targetStateInfo.description)
                            is TargetState -> {
                                // PlantUML cant draw multi-target transitions. So I have to simply loop through all the targets.
                                @Suppress("UNCHECKED_CAST")
                                for (targetState in targetStateInfo.transitionDirection.targetStates as Set<InternalState>) {
                                    crossLevelTransitions += "$stateGraphName --> ${targetState.targetGraphName()}" +
                                            transitionLabel(targetStateInfo.description)
                                }
                            }
                        }
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

        val sourceStateGraphName = transition.sourceState.graphName()
        val targetStateInfoList = executeDirectionProducerPolicy<E>(transition.metaInfo) { policy ->
            transition.produceTargetStateDirection(policy)
        }
        for (targetStateInfo in targetStateInfoList) {
            val transitionDirection = targetStateInfo.transitionDirection
            when (transitionDirection) {
                NoTransition -> continue
                Stay -> line(
                    "$sourceStateGraphName --> $sourceStateGraphName" +
                            transitionLabel(transition, targetStateInfo.description)
                )
                is TargetState -> {
                    // PlantUML cant draw multi-target transitions. So I have to simply loop through all the targets.
                    @Suppress("UNCHECKED_CAST")
                    for (targetState in transitionDirection.targetStates as Set<InternalState>) {
                        val transitionString = "$sourceStateGraphName --> ${targetState.targetGraphName()}" +
                                transitionLabel(transition, targetStateInfo.description)

                        if (transition.sourceState.isNeighbor(targetState))
                            line(transitionString)
                        else
                            crossLevelTransitions += transitionString
                    }
                }
            }
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
                            block(makeUnsafeCollectTargetStatesPolicy<E>(eventAndArgument))
                        )
                    }
                    hintMap[StateResolutionHint::class]?.mapTo(stateInfoList) {
                        it as StateResolutionHint
                        TargetStateInfo(it.description, TargetState(it.targetStates))
                    }
                } else {
                    stateInfoList += TargetStateInfo(null, block(makeUnsafeCollectTargetStatesPolicy<E>()))
                }
            } else {
                stateInfoList += TargetStateInfo(null, block(makeUnsafeCollectTargetStatesPolicy<E>()))
            }
        } else {
            stateInfoList += TargetStateInfo(null, block(CollectTargetStatesPolicy()))
            metaInfo.findMetaInfo<ExportMetaInfo>()
                ?.resolutionHints
                ?.filterIsInstance<StateResolutionHint>()
                ?.mapTo(stateInfoList) { TargetStateInfo(it.description, TargetState(it.targetStates)) }
        }
        return stateInfoList
    }

    private val MetaInfo?.hasUnsafeCallConditionalLambdas: Boolean
        get() = unsafeCallConditionalLambdas && findMetaInfo<IgnoreUnsafeCallConditionalLambdasMetaInfo>() == null

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

        // visit transitions, skipping internal StateMachines
        states.flatMap {
            if (it !is StateMachine) it.transitions else emptySet()
        }.forEach { visit(it) }

        // add finish transitions
        states.filterIsInstance<IFinalState>()
            .forEach { line("${it.graphName()} --> $STAR") }
    }

    private fun line(text: String) = builder.appendLine(SINGLE_INDENT.repeat(indent) + text)

    private fun transitionLabel(transition: Transition<*>, description: String?): String {
        val text = listOfNotNull(
            transition.metaInfo?.umlLabel ?: transition.name,
            transition.eventMatcher.eventClass.simpleName.takeIf { showEventLabels },
            description,
        ).joinToString()
        return transitionLabel(text)
    }

    private fun transitionLabel(text: String?) = " : $text".takeIf { text?.isNotBlank() == true } ?: ""

    private fun IState.printStateDescriptions() {
        val descriptions = metaInfo.findMetaInfo<UmlMetaInfo>()?.umlStateDescriptions.orEmpty()
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

private fun <E : Event> makeUnsafeCollectTargetStatesPolicy(
    @Suppress("UNCHECKED_CAST") // this is unsafe by design
    eventAndArgument: EventAndArgument<E> = EventAndArgument(ExportPlantUmlEvent as E, null)
): TransitionDirectionProducerPolicy<E> {
    return UnsafeCollectTargetStatesPolicy(eventAndArgument)
}

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