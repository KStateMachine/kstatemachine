/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.visitors

import ru.nsk.kstatemachine.state.IState

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