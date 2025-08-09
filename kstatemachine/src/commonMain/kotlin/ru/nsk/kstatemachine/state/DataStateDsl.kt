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
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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