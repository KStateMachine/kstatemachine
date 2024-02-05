package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.ChildMode.EXCLUSIVE

/** inline constructor function */
inline fun <reified D : Any> defaultDataState(
    name: String? = null,
    displayName: String? = null,
    defaultData: D? = null,
    childMode: ChildMode = EXCLUSIVE,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
) = DefaultDataState(name, displayName, defaultData, childMode, dataExtractor)

open class DefaultDataState<D : Any>(
    name: String? = null,
    displayName: String? = null,
    override val defaultData: D? = null,
    childMode: ChildMode = EXCLUSIVE,
    private val dataExtractor: DataExtractor<D>,
) : BaseStateImpl(name, displayName, childMode), DataState<D> {
    private var _data: D? = null
    override val data: D get() = checkNotNull(_data) { "Data is not set. Is $this state active?" }

    private var _lastData: D? = null
    override val lastData: D
        get() = checkNotNull(_lastData ?: defaultData) {
            "Last data is not available yet in $this, and default data not provided"
        }

    override suspend fun onDoEnter(transitionParams: TransitionParams<*>) {
        fun assign(data: D?) {
            if (data != null) {
                _data = data
                _lastData = data
            } else {
                _data = lastData
            }
        }

        if (this === transitionParams.direction.targetState) {
            when (val event = transitionParams.event) {
                is DataEvent<*> -> assignData(event)
                is WrappedEvent -> assignData(event.event)
                is FinishedEvent -> assign(dataExtractor.extractFinishedEvent(transitionParams, event))
                else -> assign(dataExtractor.extract(transitionParams))
            }
        } else { // implicit activation
            _data = lastData
        }
    }

    private fun assignData(event: Event) {
        @Suppress("UNCHECKED_CAST")
        event as DataEvent<D>
        with(event.data) {
            _data = this
            _lastData = this
        }
    }

    override suspend fun onDoExit(transitionParams: TransitionParams<*>) {
        _data = null
    }

    private fun cleanData() {
        _data = null
        _lastData = null
    }

    override suspend fun onStopped() = cleanData()
    override suspend fun onCleanup() = cleanData()
}

/** inline constructor function */
inline fun <reified D : Any> defaultFinalDataState(
    name: String? = null,
    displayName: String? = null,
    defaultData: D? = null,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
): DefaultFinalDataState<D> = DefaultFinalDataState(name, displayName, defaultData, dataExtractor)

open class DefaultFinalDataState<D : Any>(
    name: String? = null,
    displayName: String? = null,
    defaultData: D? = null,
    dataExtractor: DataExtractor<D>
) : DefaultDataState<D>(name, displayName, defaultData, EXCLUSIVE, dataExtractor), FinalDataState<D>
