package ru.nsk.kstatemachine

/**
 * Method for testing purpose.
 */
object Testing {
    /**
     * Starts machine from particular [state]
     */
    suspend fun StateMachine.startFrom(state: IState, argument: Any? = null) =
        startFrom(setOf(state), argument)

    /**
     * Vararg overload for targeting multiple states
     */
    suspend fun StateMachine.startFrom(state1: IState, state2: IState, vararg states: IState, argument: Any? = null) =
        startFrom(setOf(state1, state2, *states), argument)

    /**
     * Allows to target multiple states (they must be parallel state sub-children).
     * Implemented by [targetParallelStates]
     */
    suspend fun StateMachine.startFrom(states: Set<IState>, argument: Any? = null) =
        (this as InternalStateMachine).startFrom(states, argument)

    suspend fun StateMachine.startFrom(stateName: String, argument: Any? = null) =
        startFrom(requireState(stateName), argument)

    suspend fun <D : Any> StateMachine.startFrom(state: DataState<D>, data: D, argument: Any? = null) =
        (this as InternalStateMachine).startFrom(state, data, argument)

    fun StateMachine.startFromBlocking(state: IState, argument: Any? = null) =
        coroutineAbstraction.runBlocking { startFrom(state, argument) }

    fun StateMachine.startFromBlocking(state1: IState, state2: IState, vararg states: IState, argument: Any? = null) =
        coroutineAbstraction.runBlocking { startFrom(state1, state2, *states, argument = argument) }

    fun StateMachine.startFromBlocking(states: Set<IState>, argument: Any? = null) =
        coroutineAbstraction.runBlocking { startFrom(states, argument) }

    fun StateMachine.startFromBlocking(stateName: String, argument: Any? = null) =
        coroutineAbstraction.runBlocking { startFrom(stateName, argument) }

    fun <D : Any> StateMachine.startFromBlocking(state: DataState<D>, data: D, argument: Any? = null) =
        coroutineAbstraction.runBlocking { startFrom(state, data, argument) }
}