package ru.nsk.kstatemachine

object Testing {
    /**
     * Method for testing purpose. It allows starting machine from particular [state]
     */
    suspend fun StateMachine.startFrom(state: IState, argument: Any? = null) =
        (this as InternalStateMachine).startFrom(state, argument)

    suspend fun StateMachine.startFrom(stateName: String, argument: Any? = null) =
        startFrom(requireState(stateName), argument)

    suspend fun <D : Any> StateMachine.startFrom(state: DataState<D>, data: D, argument: Any? = null) =
        (this as InternalStateMachine).startFrom(state, data, argument)

    fun StateMachine.startFromBlocking(state: IState, argument: Any? = null) =
        coroutineAbstraction.runBlocking { startFrom(state, argument) }

    fun StateMachine.startFromBlocking(stateName: String, argument: Any? = null) =
        coroutineAbstraction.runBlocking { startFrom(stateName, argument) }

    fun <D : Any> StateMachine.startFromBlocking(state: DataState<D>, data: D, argument: Any? = null) =
        coroutineAbstraction.runBlocking { startFrom(state, data, argument) }
}