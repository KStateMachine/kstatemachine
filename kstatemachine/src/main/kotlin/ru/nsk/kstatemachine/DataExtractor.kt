package ru.nsk.kstatemachine

/**
 * Allows to extract data for [DataState] from any [Event]
 *
 * [FinishedEvent] handling is separated into special method as it is very easy to forget to handle it in single method
 * when implementing custom [DataExtractor].
 */
interface DataExtractor<D : Any> {
    suspend fun extractFinishedEvent(transitionParams: TransitionParams<*>, event: FinishedEvent): D?
    suspend fun extract(transitionParams: TransitionParams<*>): D?
}

inline fun <reified D : Any> defaultDataExtractor() = object : DataExtractor<D> {
    override suspend fun extractFinishedEvent(transitionParams: TransitionParams<*>, event: FinishedEvent) = event.data as? D
    override suspend fun extract(transitionParams: TransitionParams<*>) = null
}