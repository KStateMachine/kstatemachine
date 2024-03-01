package ru.nsk.kstatemachine.state

import ru.nsk.kstatemachine.state.ChildMode.EXCLUSIVE

/**
 * The most common state
 */
open class DefaultState(name: String? = null, childMode: ChildMode = EXCLUSIVE) :
    BaseStateImpl(name, childMode), State

open class DefaultFinalState(name: String? = null) : DefaultState(name), FinalState