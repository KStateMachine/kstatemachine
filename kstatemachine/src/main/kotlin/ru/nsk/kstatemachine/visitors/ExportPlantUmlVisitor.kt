package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.*

/**
 * Export state machine to Plant UML language format.
 * @see <a href="https://plantuml.com/ru/state-diagram">Plant UML state diagram</a>
 *
 * Conditional transitions depending on external data not currently supported.
 */
class ExportPlantUmlVisitor : Visitor {
    private val builder = StringBuilder()
    private var indent = 0
    private val crossLevelTransitions = mutableListOf<String>()

    fun export() = builder.toString()

    override fun visit(machine: StateMachine) {
        line("@startuml")
        line("hide empty description")

        processStateBody(machine)
        crossLevelTransitions.forEach { line(it) }

        line("@enduml")
    }

    override fun visit(state: IState) {
        if (state.states.isEmpty()) {
            line("state ${state.graphName()}")
        } else {
            line("state ${state.graphName()} {")
            ++indent
            processStateBody(state)
            --indent
            line("}")
        }
    }

    /**
     * PlantUML cannot show correctly cross level transitions to nested states.
     * It requires to see all states declarations first to provide correct rendering,
     * so we have to store them to print after state declaration.
     */
    override fun visit(transition: Transition<*>) {
        transition as InternalTransition<*>

        val sourceState = transition.sourceState.graphName()
        val targetState = transition.produceTargetStateDirection().targetState ?: return

        val transitionString = "$sourceState --> ${targetState.graphName()}${label(transition.name)}"

        if (transition.sourceState.isNeighbor(targetState))
            line(transitionString)
        else
            crossLevelTransitions.add(transitionString)
    }

    private fun processStateBody(state: IState) {
        val states = state.states
        // visit states
        states.forEach { visit(it) }

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

    private companion object {
        const val STAR = "[*]"
        const val SINGLE_INDENT = "    "
        fun IState.graphName() = name?.replace(" ", "_") ?: "State${hashCode()}"
        fun label(name: String?) = if (name != null) " : $name" else ""
    }
}

fun StateMachine.exportToPlantUml() = with(ExportPlantUmlVisitor()) {
    accept(this)
    export()
}
