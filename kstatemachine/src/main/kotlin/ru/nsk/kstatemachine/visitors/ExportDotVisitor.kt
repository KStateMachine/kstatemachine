package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.*

/**
 * Export state machine to Graphviz DOT language format.
 * @see <a href="https://graphviz.org/doc/info/lang.html">Graphviz DOT language</a>
 *
 * Exporting nested states is not currently supported use [exportToPlantUml].
 * Conditional transitions depending on external data not currently supported.
 */
class ExportDotVisitor : Visitor {
    private val builder = StringBuilder()

    fun export() = builder.toString()

    override fun visit(machine: StateMachine) {
        line("digraph state_machine {")
        machine.name?.let { builder.appendLine("    label=\"$it\";") }

        // visit states
        machine.states.forEach { visit(it) }

        // add initial transition
        line("")
        val initialState = machine.initialState
        if (initialState != null)
            line("    $INITIAL -> ${initialState.graphName()};")

        // visit transitions
        machine.states.flatMap { it.transitions }.forEach { visit(it) }

        // add finish transitions
        val finalStates = machine.states.filterIsInstance<FinalState>()
        finalStates.forEach { line("    ${it.graphName()} -> $FINISH;") }

        // add initial and finish nodes
        line("")
        if (initialState != null) line("    $INITIAL [ shape = point ];")
        if (finalStates.isNotEmpty()) line("    $FINISH [ shape = point ];")

        line("}")
    }

    override fun visit(state: State) {
        line("    ${state.graphName()};")
    }

    override fun visit(transition: Transition<*>) {
        transition as InternalTransition<*>

        val sourceState = transition.sourceState.graphName()
        val targetState = transition.produceTargetStateDirection().targetState ?: return

        line("    $sourceState -> $targetState${label(transition.name)};")
    }

    private fun line(text: String) = builder.appendLine(text)

    private companion object {
        const val INITIAL = "INITIAL"
        const val FINISH = "FINISH"
        fun State.graphName() = name?.replace(" ", "_") ?: "State${hashCode()}"
        fun label(name: String?) = if (name != null) " [ label = \"$name\" ]" else ""
    }
}

fun StateMachine.exportToDot() = with(ExportDotVisitor()) {
    accept(this)
    export()
}