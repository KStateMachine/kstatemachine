package ru.nsk.kstatemachine

/**
 * Allows to extract data for [DataState] from any [Event]
 *
 * [FinishedEvent] handling is separated into special method as it is very easy to forget to handle it in single method
 * when implementing custom [DataExtractor].
 */
interface DataExtractor<D : Any> {
    fun extractFinishedEvent(transitionParams: TransitionParams<*>, event: FinishedEvent): D?
    fun extract(transitionParams: TransitionParams<*>): D?
}

inline fun <reified D : Any> defaultDataExtractor() = object : DataExtractor<D> {
    override fun extractFinishedEvent(transitionParams: TransitionParams<*>, event: FinishedEvent) = event.data as? D
    override fun extract(transitionParams: TransitionParams<*>) = null
}