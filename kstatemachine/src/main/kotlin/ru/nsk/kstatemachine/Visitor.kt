package ru.nsk.kstatemachine

/**
 * Interface for visiting state machine components
 */
interface Visitor {
    fun visit(machine: StateMachine)
    fun visit(state: State)
    fun visit(transition: Transition<*>)
}

interface VisitorAcceptor {
    fun accept(visitor: Visitor)
}

/**
 * Export state machine to Graphviz DOT language format.
 * @see <a href="https://graphviz.org/doc/info/lang.html">Graphviz DOT language</a>
 *
 * Conditional transitions depending on external data not currently supported.
 */
class ExportDotVisitor : Visitor {
    private val builder = StringBuilder()

    fun export() = builder.toString()

    override fun visit(machine: StateMachine) {
        builder.appendLine("digraph state_machine {")
        machine.name?.let { builder.appendLine("    label=\"$it\";") }

        // add states
        machine.states.forEach { visit(it) }

        // add initial transition
        builder.appendLine()
        val initialState = machine.initialState
        if (initialState != null) builder.appendLine("    $INITIAL -> ${initialState.graphName()};")

        // add transitions
        machine.states.forEach { it.transitions.forEach { transition -> visit(transition) } }

        // add finish transitions
        val finalStates = machine.states.filterIsInstance<FinalState>()
        finalStates.forEach {
            builder.appendLine("    ${it.graphName()} -> $FINISH;")
        }

        // add initial and finish nodes
        builder.appendLine()
        if (initialState != null) builder.appendLine("    $INITIAL [ shape = point ];")
        if (finalStates.isNotEmpty()) builder.appendLine("    $FINISH [ shape = point ];")

        builder.appendLine("}")
    }

    override fun visit(state: State) {
        builder.appendLine("    ${state.graphName()};")
    }

    override fun visit(transition: Transition<*>) {
        val internalTransition = transition as InternalTransition<*>

        val sourceState = internalTransition.sourceState.graphName()
        val targetState = when (val direction = internalTransition.produceTargetStateDirection()) {
            Stay -> return
            NoTransition -> return
            is TargetState -> direction.targetState.graphName()
        }
        builder.appendLine("    $sourceState -> $targetState${label(internalTransition.name)};")
    }

    private companion object {
        const val INITIAL = "INITIAL"
        const val FINISH = "FINISH"
        fun State.graphName() = name ?: "State${hashCode()}"
        fun label(name: String?) = if (name != null) " [ label = \"$name\" ]" else ""
    }
}

fun StateMachine.exportToDot() = with(ExportDotVisitor()) {
    accept(this)
    export()
}
