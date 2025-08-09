/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import ru.nsk.kstatemachine.event.DataExtractor
import ru.nsk.kstatemachine.event.defaultDataExtractor
import ru.nsk.kstatemachine.state.ChildMode.EXCLUSIVE

/** inline constructor function */
inline fun <reified D : Any> defaultMutableDataState(
    name: String? = null,
    defaultData: D? = null,
    childMode: ChildMode = EXCLUSIVE,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
) = DefaultMutableDataState(name, defaultData, childMode, dataExtractor)

open class DefaultMutableDataState<D : Any>(
    name: String? = null,
    defaultData: D? = null,
    childMode: ChildMode = EXCLUSIVE,
    dataExtractor: DataExtractor<D>,
) : DefaultDataState<D>(name, defaultData, childMode, dataExtractor), MutableDataState<D> {
    override fun setData(data: D) {
        if (isActive) _data = data
        _lastData = data
    }
}

/** inline constructor function */
inline fun <reified D : Any> defaultFinalMutableDataState(
    name: String? = null,
    defaultData: D? = null,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
) = DefaultFinalMutableDataState(name, defaultData, dataExtractor)

open class DefaultFinalMutableDataState<D : Any>(
    name: String? = null,
    defaultData: D? = null,
    dataExtractor: DataExtractor<D>,
) : DefaultMutableDataState<D>(name, defaultData, EXCLUSIVE, dataExtractor), FinalMutableDataState<D>
