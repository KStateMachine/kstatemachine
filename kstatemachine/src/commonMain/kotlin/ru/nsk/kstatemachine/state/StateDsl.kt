/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

@file:OptIn(ExperimentalContracts::class)

package ru.nsk.kstatemachine.state

import ru.nsk.kstatemachine.event.DataExtractor
import ru.nsk.kstatemachine.event.defaultDataExtractor
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

inline fun <reified D : Any> IState.dataState(
    name: String? = null,
    defaultData: D? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
): DataState<D> = addState(defaultDataState(name, defaultData, childMode, dataExtractor))

suspend inline fun <reified D : Any> IState.dataState(
    name: String? = null,
    defaultData: D? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
    init: StateBlock<DataState<D>>
): DataState<D> {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return addState(defaultDataState(name, defaultData, childMode, dataExtractor), init)
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
 * @param defaultData is necessary for initial [DataState]
 */
inline fun <reified D : Any> IState.initialDataState(
    name: String? = null,
    defaultData: D,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
): DataState<D> = addInitialState(defaultDataState(name, defaultData, childMode, dataExtractor))

/**
 * @param defaultData is necessary for initial [DataState]
 */
suspend inline fun <reified D : Any> IState.initialDataState(
    name: String? = null,
    defaultData: D,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
    init: StateBlock<DataState<D>>
): DataState<D> {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return addInitialState(defaultDataState(name, defaultData, childMode, dataExtractor), init)
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

inline fun <reified D : Any> IState.finalDataState(
    name: String? = null,
    defaultData: D? = null,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
): FinalDataState<D> = addFinalState(defaultFinalDataState(name, defaultData, dataExtractor))

suspend inline fun <reified D : Any> IState.finalDataState(
    name: String? = null,
    defaultData: D? = null,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
    init: StateBlock<FinalDataState<D>>
): FinalDataState<D> {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return addFinalState(defaultFinalDataState(name, defaultData, dataExtractor), init)
}

inline fun <reified D : Any> IState.initialFinalDataState(
    name: String? = null,
    defaultData: D? = null,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
): FinalDataState<D> = addInitialState(defaultFinalDataState(name, defaultData, dataExtractor))

suspend inline fun <reified D : Any> IState.initialFinalDataState(
    name: String? = null,
    defaultData: D? = null,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
    init: StateBlock<FinalDataState<D>>
): FinalDataState<D> {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return addInitialState(defaultFinalDataState(name, defaultData, dataExtractor), init)
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