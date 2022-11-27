package ru.nsk.kstatemachine

/**
 * Allows to extract data for [DataState] from any [Event]
 */
interface DataExtractor<D : Any> {
    fun extractFinishedEvent(transitionParams: TransitionParams<*>, event: FinishedEvent): D?
    fun extract(transitionParams: TransitionParams<*>): D?
}

inline fun <reified D : Any> defaultDataExtractor() = object : DataExtractor<D> {
    override fun extractFinishedEvent(transitionParams: TransitionParams<*>, event: FinishedEvent) = event.data as? D
    override fun extract(transitionParams: TransitionParams<*>) = null
}