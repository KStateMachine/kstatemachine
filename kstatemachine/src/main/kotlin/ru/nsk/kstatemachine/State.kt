package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.visitors.Visitor
import ru.nsk.kstatemachine.visitors.VisitorAcceptor

@StateMachineDslMarker
interface State : TransitionsStateHelper, VisitorAcceptor {
    val name: String?
    val states: Set<State>
    val initialState: State?
    val parent: State?
    val machine: StateMachine
    val isActive: Boolean

    fun <L : Listener> addListener(listener: L): L
    fun removeListener(listener: Listener)

    fun <S : State> addState(state: S, init: StateBlock<S>? = null): S

    /**
     * Get state by name. This might be used to start listening to state after state machine setup.
     */
    fun findState(name: String): State?

    /**
     * Currently initial state is mandatory, but if we add parallel states it might change.
     */
    fun setInitialState(state: State)

    override fun accept(visitor: Visitor) = visitor.visit(this)

    interface Listener {
        fun onEntry(transitionParams: TransitionParams<*>) = Unit
        fun onExit(transitionParams: TransitionParams<*>) = Unit

        /**
         * Notifies that child [FinalState] is entered.
         */
        fun onFinished() = Unit
    }
}

/**
 * When [StateMachine] enters this state it finishes and does not accept events any more.
 */
interface FinalState : InternalState

/**
 * Defines state API for internal library usage. All states must implement this interface.
 */
interface InternalState : State {
    override var parent: InternalState?

    fun isNeighbor(state: State): Boolean
    fun notify(block: State.Listener.() -> Unit)
    fun <E : Event> findTransitionsByEvent(event: E): List<InternalTransition<E>>

    fun doEnter(transitionParams: TransitionParams<*>)
    fun doExit(transitionParams: TransitionParams<*>)

    fun recursiveEnterInitialState()
    fun recursiveEnterStatePath(path: MutableList<InternalState>, transitionParams: TransitionParams<*>)
    fun recursiveExit(transitionParams: TransitionParams<*>)

    /** @return true if event was processed */
    fun recursiveProcessEvent(event: Event, argument: Any?): Boolean
}

fun InternalState.requireParent() = requireNotNull(parent) { "Parent is not set" }

fun State.requireState(name: String) = requireNotNull(findState(name)) { "State $name not found" }

operator fun <S : State> S.invoke(block: S.() -> Unit) = block()

fun <S : State> S.onEntry(block: S.(TransitionParams<*>) -> Unit) {
    addListener(object : State.Listener {
        override fun onEntry(transitionParams: TransitionParams<*>) = block(transitionParams)
    })
}

fun <S : State> S.onExit(block: S.(TransitionParams<*>) -> Unit) {
    addListener(object : State.Listener {
        override fun onExit(transitionParams: TransitionParams<*>) = block(transitionParams)
    })
}

fun <S : State> S.onFinished(block: S.() -> Unit) {
    addListener(object : State.Listener {
        override fun onFinished() = block()
    })
}

/**
 * @param name is optional and is useful for getting state instance after state machine setup
 * with [State.findState] and for debugging.
 */
fun State.state(name: String? = null, init: StateBlock<State>? = null) =
    addState(DefaultState(name), init)

/**
 * A shortcut for [state] and [State.setInitialState] calls
 */
fun State.initialState(name: String? = null, init: StateBlock<State>? = null) =
    addInitialState(DefaultState(name), init)

/**
 * A shortcut for [State.addState] and [State.setInitialState] calls
 */
fun <S : State> State.addInitialState(state: S, init: StateBlock<S>? = null): S {
    addState(state, init)
    setInitialState(state)
    return state
}

/**
 * Helper method for adding final states. This is exactly the same as simply call [State.addState] but makes
 * code more self expressive.
 */
fun <S : FinalState> State.addFinalState(state: S, init: StateBlock<S>? = null) = addState(state, init)

fun State.finalState(name: String? = null, init: StateBlock<FinalState>? = null) =
    addState(DefaultFinalState(name), init)
