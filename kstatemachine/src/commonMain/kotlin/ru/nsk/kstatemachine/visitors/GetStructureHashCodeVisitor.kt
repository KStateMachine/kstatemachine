package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.DataState
import ru.nsk.kstatemachine.state.HistoryState
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.transition.Transition

/**
 * This visitor collects structural information about [StateMachine] in order to compare two [StateMachine]
 * instances for structural equality.
 * There is no guaranty that his class will find the difference in two really different machines,
 * but it tries to do its best.
 */
internal class GetStructureHashCodeVisitor : RecursiveVisitor {
    private val records = mutableListOf<String>()
    val structureHashCode get() = records.hashCode()

    override fun visit(machine: StateMachine) {
        records += machine.stateInfo()
        records += "StateMachine creationArguments:${machine.creationArguments}"
        machine.visitChildren()
    }

    override fun visit(state: IState) {
        if (state !is StateMachine) { // do not check nested machines
            records += state.stateInfo()
            state.visitChildren()
        } else {
            records += "class:${state::class.simpleName}, name:${state.name}"
        }
    }

    private fun IState.stateInfo() = "class:${this::class.simpleName}, " +
            "name:$name, " +
            "statesCount:${states.size}, " +
            "transitionsCount:${transitions.size}, " +
            "childMode:$childMode" +
            if (this is HistoryState) ", historyType:$historyType" else "" +
                    if (this is DataState<*>) ", dataClass:$dataClass, defaultData:$defaultData" else ""

    override fun <E : Event> visit(transition: Transition<E>) {
        records += "class:${transition::class.simpleName}, " +
                "name:${transition.name}, " +
                "type:${transition.type}, " +
                "event:${transition.eventMatcher.eventClass}"
    }
}

fun StateMachine.getStructureHashCode() = with(GetStructureHashCodeVisitor()) {
    visit(this@getStructureHashCode)
    structureHashCode
}