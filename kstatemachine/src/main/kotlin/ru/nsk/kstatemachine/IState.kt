package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.visitors.Visitor
import ru.nsk.kstatemachine.visitors.VisitorAcceptor

@StateMachineDslMarker
interface IState : StateTransitionsHelper, VisitorAcceptor {
    val name: String?
    val states: Set<IState>
    val initialState: IState?
    val parent: IState?
    val machine: StateMachine
    val isActive: Boolean
    val listeners: Collection<Listener>

    fun <L : Listener> addListener(listener: L): L
    fun removeListener(listener: Listener)

    fun <S : IState> addState(state: S, init: StateBlock<S>? = null): S

    /**
     * Currently initial state is mandatory, but if we add parallel states it might change.
     */
    fun setInitialState(state: IState)

    override fun accept(visitor: Visitor) = visitor.visit(this)

    interface Listener {
        fun onEntry(transitionParams: TransitionParams<*>) = Unit
        fun onExit(transitionParams: TransitionParams<*>) = Unit

        /**
         * Notifies that child [IFinalState] is entered.
         */
        fun onFinished() = Unit
    }
}

/**
 * Simple state without data field that is used by typesafe transitions
 */
interface State : IState

/**
 * State which holds data while it is active
 */
interface DataState<out D> : IState {
    /**
     * This property might be accessed only while this state is active
     */
    val data: D
}

/**
 * When [StateMachine] enters this state it finishes and does not accept events any more.
 */
interface IFinalState : IState
interface FinalState : IFinalState, State
interface FinalDataState<out D> : IFinalState, DataState<D>

typealias StateBlock<S> = S.() -> Unit

/**
 * Get state by name. This might be used to start listening to state after state machine setup.
 */
fun IState.findState(name: String): IState? = states.find { it.name == name }

fun IState.requireState(name: String) = requireNotNull(findState(name)) { "State $name not found" }

operator fun <S : IState> S.invoke(block: StateBlock<S>) = block()

fun <S : IState> S.onEntry(block: S.(TransitionParams<*>) -> Unit) {
    addListener(object : IState.Listener {
        override fun onEntry(transitionParams: TransitionParams<*>) = block(transitionParams)
    })
}

fun <S : IState> S.onExit(block: S.(TransitionParams<*>) -> Unit) {
    addListener(object : IState.Listener {
        override fun onExit(transitionParams: TransitionParams<*>) = block(transitionParams)
    })
}

fun <S : IState> S.onFinished(block: StateBlock<S>) {
    addListener(object : IState.Listener {
        override fun onFinished() = block()
    })
}

/**
 * @param name is optional and is useful for getting state instance after state machine setup
 * with [IState.findState] and for debugging.
 */
fun IState.state(name: String? = null, init: StateBlock<State>? = null) =
    addState(DefaultState(name), init)

fun <D> IState.dataState(name: String? = null, init: StateBlock<DataState<D>>? = null) =
    addState(DefaultDataState(name), init)

/**
 * A shortcut for [state] and [IState.setInitialState] calls
 */
fun IState.initialState(name: String? = null, init: StateBlock<State>? = null) =
    addInitialState(DefaultState(name), init)

/**
 * A shortcut for [IState.addState] and [IState.setInitialState] calls
 */
fun <S : IState> IState.addInitialState(state: S, init: StateBlock<S>? = null): S {
    addState(state, init)
    setInitialState(state)
    return state
}

/**
 * Helper method for adding final states. This is exactly the same as simply call [IState.addState] but makes
 * code more self expressive.
 */
fun <S : IFinalState> IState.addFinalState(state: S, init: StateBlock<S>? = null) =
    addState(state, init)

fun IState.finalState(name: String? = null, init: StateBlock<FinalState>? = null) =
    addState(DefaultFinalState(name), init)

fun <D> IState.finalDataState(name: String? = null, init: StateBlock<FinalDataState<D>>? = null) =
    addState(DefaultFinalDataState(name), init)
