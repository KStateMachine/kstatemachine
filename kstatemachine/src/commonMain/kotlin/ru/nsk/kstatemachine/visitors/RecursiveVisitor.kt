package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.IState

internal interface RecursiveVisitor : Visitor {
    fun IState.visitChildren() {
        transitions.forEach { visit(it) }
        states.forEach { visit(it) }
    }
}

internal interface RecursiveCoVisitor : CoVisitor {
    suspend fun IState.visitChildren() {
        transitions.forEach { visit(it) }
        states.forEach { visit(it) }
    }
}