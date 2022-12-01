package ru.nsk.kstatemachine

object Testing {
    /**
     * Method for testing purpose. It allows starting machine from particular [state]
     */
    fun StateMachine.startFrom(state: IState, argument: Any? = null) =
        (this as InternalStateMachine).startFrom(state, argument)

    fun StateMachine.startFrom(stateName: String, argument: Any? = null) =
        startFrom(requireState(stateName), argument)

    fun <D : Any> StateMachine.startFrom(state: DataState<D>, data: D, argument: Any? = null) =
        (this as InternalStateMachine).startFrom(state, data, argument)
}