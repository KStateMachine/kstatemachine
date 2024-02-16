package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.TransitionDirectionProducerPolicy.CollectTargetStatesPolicy
import ru.nsk.kstatemachine.TransitionDirectionProducerPolicy.UnsafeCollectTargetStatesPolicy
import ru.nsk.kstatemachine.visitors.CompatibilityFormat.MERMAID
import ru.nsk.kstatemachine.visitors.CompatibilityFormat.PLANT_UML

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
 * Uses [IUmlMetaInfo] for output sugaring.
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

        processStateBody(machine)
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
                    targetStates.forEach { targetState ->
                        crossLevelTransitions += "${state.graphName()} --> ${targetState.targetGraphName()}"
                    }
                }
                else -> {
                    line("state ${state.labelGraphName()}")
                    state.printStateDescriptions()
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
        val stateDescriptions = (metaInfo as? IUmlMetaInfo)?.stateDescriptions.orEmpty()
        stateDescriptions.forEach { line("${graphName()} : $it") }
    }

    private companion object {
        val MetaInfo.umlLabel get() = (this as? IUmlMetaInfo)?.umlLabel

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