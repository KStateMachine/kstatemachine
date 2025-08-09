/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

@file:OptIn(ExperimentalContracts::class)

package ru.nsk.kstatemachine.state

import ru.nsk.kstatemachine.state.pseudo.DefaultChoiceDataState
import ru.nsk.kstatemachine.state.pseudo.DefaultChoiceState
import ru.nsk.kstatemachine.state.pseudo.DefaultHistoryState
import ru.nsk.kstatemachine.transition.EventAndArgument
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * @param name is optional and is useful for getting state instance after state machine setup
 * with [IState.findState] and for debugging.
 */
fun IState.state(
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
): State = addState(DefaultState(name, childMode))

/**
 * @param name is optional and is useful for getting state instance after state machine setup
 * with [IState.findState] and for debugging.
 */
suspend inline fun IState.state(
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    init: StateBlock<State>
): State {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return addState(DefaultState(name, childMode), init)
}

/**
 * A shortcut for [state] and [IState.setInitialState] calls
 */
fun IState.initialState(
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
): State = addInitialState(DefaultState(name, childMode))

/**
 * A shortcut for [state] and [IState.setInitialState] calls
 */
suspend inline fun IState.initialState(
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    init: StateBlock<State>
): State {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return addInitialState(DefaultState(name, childMode), init)
}

/**
 * A shortcut for [IState.addState] and [IState.setInitialState] calls
 */
fun <S : IState> IState.addInitialState(state: S): S {
    addState(state)
    setInitialState(state)
    return state
}

/**
 * A shortcut for [IState.addState] and [IState.setInitialState] calls
 */
suspend inline fun <S : IState> IState.addInitialState(state: S, init: StateBlock<S>): S {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    addState(state, init)
    setInitialState(state)
    return state
}

/**
 * Helper dsl method for adding final states. This is exactly the same as simply call [IState.addState] but makes
 * code more self expressive.
 */
fun <S : IFinalState> IState.addFinalState(state: S) = addState(state)

/**
 * Helper dsl method for adding final states. This is exactly the same as simply call [IState.addState] but makes
 * code more self expressive.
 */
suspend inline fun <S : IFinalState> IState.addFinalState(state: S, init: StateBlock<S>): S {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return addState(state, init)
}

fun IState.finalState(name: String? = null) = addFinalState(DefaultFinalState(name))

suspend inline fun IState.finalState(name: String? = null, init: StateBlock<FinalState>): FinalState {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return addFinalState(DefaultFinalState(name), init)
}

fun IState.initialFinalState(name: String? = null) = addInitialState(DefaultFinalState(name))

suspend inline fun IState.initialFinalState(name: String? = null, init: StateBlock<FinalState>): FinalState {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return addInitialState(DefaultFinalState(name), init)
}

fun IState.choiceState(
    name: String? = null,
    choiceAction: suspend EventAndArgument<*>.() -> State
) = addState(DefaultChoiceState(name, choiceAction = choiceAction))

fun IState.initialChoiceState(
    name: String? = null,
    choiceAction: suspend EventAndArgument<*>.() -> State
) = addInitialState(DefaultChoiceState(name, choiceAction = choiceAction))

inline fun <reified D : Any> IState.choiceDataState(
    name: String? = null,
    noinline choiceAction: suspend EventAndArgument<*>.() -> DataState<D>
) = addState(DefaultChoiceDataState(name, D::class, choiceAction = choiceAction))

inline fun <reified D : Any> IState.initialChoiceDataState(
    name: String? = null,
    noinline choiceAction: suspend EventAndArgument<*>.() -> DataState<D>
) = addInitialState(DefaultChoiceDataState(name, D::class, choiceAction = choiceAction))

fun IState.historyState(
    name: String? = null,
    defaultState: IState? = null,
    historyType: HistoryType = HistoryType.SHALLOW,
): HistoryState = addState(DefaultHistoryState(name, defaultState, historyType))