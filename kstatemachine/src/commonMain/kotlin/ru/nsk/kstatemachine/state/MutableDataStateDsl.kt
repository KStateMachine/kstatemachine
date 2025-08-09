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

inline fun <reified D : Any> IState.mutableDataState(
    name: String? = null,
    defaultData: D? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
): MutableDataState<D> = addState(defaultMutableDataState(name, defaultData, childMode, dataExtractor))

suspend inline fun <reified D : Any> IState.mutableDataState(
    name: String? = null,
    defaultData: D? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
    init: StateBlock<MutableDataState<D>>
): MutableDataState<D> {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return addState(defaultMutableDataState(name, defaultData, childMode, dataExtractor), init)
}

/**
 * @param defaultData is necessary for initial [DataState]
 */
inline fun <reified D : Any> IState.initialMutableDataState(
    name: String? = null,
    defaultData: D,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
): MutableDataState<D> = addInitialState(defaultMutableDataState(name, defaultData, childMode, dataExtractor))

/**
 * @param defaultData is necessary for initial [DataState]
 */
suspend inline fun <reified D : Any> IState.initialMutableDataState(
    name: String? = null,
    defaultData: D,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
    init: StateBlock<MutableDataState<D>>
): MutableDataState<D> {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return addInitialState(defaultMutableDataState(name, defaultData, childMode, dataExtractor), init)
}

inline fun <reified D : Any> IState.finalMutableDataState(
    name: String? = null,
    defaultData: D? = null,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
): FinalMutableDataState<D> = addFinalState(defaultFinalMutableDataState(name, defaultData, dataExtractor))

suspend inline fun <reified D : Any> IState.finalMutableDataState(
    name: String? = null,
    defaultData: D? = null,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
    init: StateBlock<FinalMutableDataState<D>>
): FinalMutableDataState<D> {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return addFinalState(defaultFinalMutableDataState(name, defaultData, dataExtractor), init)
}

inline fun <reified D : Any> IState.initialFinalMutableDataState(
    name: String? = null,
    defaultData: D? = null,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
): FinalMutableDataState<D> = addInitialState(defaultFinalMutableDataState(name, defaultData, dataExtractor))

suspend inline fun <reified D : Any> IState.initialFinalMutableDataState(
    name: String? = null,
    defaultData: D? = null,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
    init: StateBlock<FinalMutableDataState<D>>
): FinalMutableDataState<D> {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return addInitialState(defaultFinalMutableDataState(name, defaultData, dataExtractor), init)
}