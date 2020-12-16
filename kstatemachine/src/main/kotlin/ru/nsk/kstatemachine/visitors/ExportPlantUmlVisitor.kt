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

    fun export() = builder.toString()

    override fun visit(machine: StateMachine) {
        line("@startuml")
        line("hide empty description")

        processStateBody(machine)

        line("@enduml")
    }

    override fun visit(state: State) {
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

    override fun visit(transition: Transition<*>) {
        val internalTransition = transition as InternalTransition<*>

        val sourceState = internalTransition.sourceState.graphName()
        val targetState = when (val direction = internalTransition.produceTargetStateDirection()) {
            Stay -> return
            NoTransition -> return
            is TargetState -> direction.targetState.graphName()
        }
        line("$sourceState --> $targetState${label(internalTransition.name)}")
    }

    private fun processStateBody(state: State) {
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
        states.filterIsInstance<FinalState>()
            .forEach { line("${it.graphName()} --> $STAR") }
    }

    private fun line(text: String) = builder.appendLine(SINGLE_INDENT.repeat(indent) + text)

    private companion object {
        const val STAR = "[*]"
        const val SINGLE_INDENT = "    "
        fun State.graphName() = name?.replace(" ", "_") ?: "State${hashCode()}"
        fun label(name: String?) = if (name != null) " : $name" else ""
    }
}

fun StateMachine.exportToPlantUml() = with(ExportPlantUmlVisitor()) {
    accept(this)
    export()
}
