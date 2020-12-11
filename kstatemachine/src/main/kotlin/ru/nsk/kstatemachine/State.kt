package ru.nsk.kstatemachine

@StateMachineDslMarker
interface State : VisitorAcceptor {
    val name: String?
    val states: Set<State>
    val initialState: State?
    val transitions: Set<Transition<*>>

    fun <E : Event> addTransition(transition: Transition<E>): Transition<E>

    fun <L : Listener> addListener(listener: L): L
    fun removeListener(listener: Listener)

    fun <S : State> addState(state: S, init: StateBlock? = null): S

    /**
     * Currently initial state is mandatory, but if we add parallel states it might change.
     */
    fun setInitialState(state: State)

    /**
     * Get state by name. This might be used to start listening to state after state machine setup.
     */
    fun findState(name: String): State?
    fun requireState(name: String): State

    /**
     * Get transition by name. This might be used to start listening to transition after state machine setup.
     */
    fun findTransition(name: String) = transitions.find { it.name == name }
    fun requireTransition(name: String) =
        findTransition(name) ?: throw IllegalArgumentException("Transition $name not found")

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }

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
    fun notify(block: State.Listener.() -> Unit)
    fun <E : Event> findTransitionByEvent(event: E): InternalTransition<E>?
}

operator fun <S : State> S.invoke(block: S.() -> Unit) = block()

/**
 * Get transition by Event class. This might be used to start listening to transition after state machine setup.
 */
inline fun <reified E : Event> State.findTransition(): Transition<E>? {
    @Suppress("UNCHECKED_CAST")
    return transitions.find { it.eventMatcher.eventClass == E::class } as Transition<E>?
}

inline fun <reified E : Event> State.requireTransition() =
    findTransition<E>() ?: throw IllegalArgumentException("Transition for ${E::class} not found")

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
fun State.state(name: String? = null, init: StateBlock? = null) =
    addState(DefaultState(name), init)

/**
 * A shortcut for [state] and [State.setInitialState] calls
 */
fun State.initialState(name: String? = null, init: StateBlock? = null) =
    addInitialState(DefaultState(name), init)

/**
 * A shortcut for [State.addState] and [State.setInitialState] calls
 */
fun <S : State> State.addInitialState(state: S, init: StateBlock? = null): S {
    addState(state, init)
    setInitialState(state)
    return state
}

fun State.finalState(name: String? = null, init: StateBlock? = null) =
    addState(DefaultFinalState(name), init)

/**
 * Creates simple transition.
 */
inline fun <reified E : Event> State.transition(
    name: String? = null,
    block: (SimpleTransitionBuilder<E>.() -> Unit),
): Transition<E> {
    val builder = SimpleTransitionBuilder<E>().apply {
        eventMatcher = isInstanceOf()
        block()
    }

    val transition = DefaultTransition(builder.eventMatcher, this, builder.targetState, name)
    builder.listener?.let { transition.addListener(it) }
    return addTransition(transition)
}

/**
 * Overload for transition without any parameters.
 */
inline fun <reified E : Event> State.transition(
    name: String? = null,
): Transition<E> =
    addTransition(DefaultTransition(EventMatcher.isInstanceOf(), this, name))

/**
 * Creates conditional transition. Caller should specify lambda which calculates [TransitionDirection].
 * For example target state may be different depending on some condition.
 */
inline fun <reified E : Event> State.transitionConditionally(
    name: String? = null,
    block: ConditionalTransitionBuilder<E>.() -> Unit,
): Transition<E> {
    val builder = ConditionalTransitionBuilder<E>().apply {
        eventMatcher = isInstanceOf()
        block()
    }

    val transition = DefaultTransition(builder.eventMatcher, this, builder.direction, name)
    builder.listener?.let { transition.addListener(it) }
    return addTransition(transition)
}

/**
 * Creates guarded transition. Such transition is triggered only when guard function returns true.
 * Same behaviour might be achieved with conditional transition but guarded transition has simpler syntax.
 */
inline fun <reified E : Event> State.transitionGuarded(
    name: String? = null,
    block: GuardedTransitionBuilder<E>.() -> Unit,
): Transition<E> {
    val builder = GuardedTransitionBuilder<E>().apply {
        eventMatcher = isInstanceOf()
        block()
    }

    val direction = {
        if (builder.guard()) {
            val target = builder.targetState
            if (target == null) stay() else targetState(target)
        } else {
            noTransition()
        }
    }

    val transition = DefaultTransition(builder.eventMatcher, this, direction, name)
    builder.listener?.let { transition.addListener(it) }
    return addTransition(transition)
}