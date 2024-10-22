/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.state

import ru.nsk.kstatemachine.*
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.event.FinishedEvent
import ru.nsk.kstatemachine.metainfo.MetaInfo
import ru.nsk.kstatemachine.state.ChildMode.EXCLUSIVE
import ru.nsk.kstatemachine.state.ChildMode.PARALLEL
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.destroyBlocking
import ru.nsk.kstatemachine.statemachine.machineNotify
import ru.nsk.kstatemachine.transition.*
import ru.nsk.kstatemachine.transition.TransitionType.EXTERNAL

/**
 * Base [IState] implementation for all states
 */
open class BaseStateImpl(
    override val name: String?,
    override val childMode: ChildMode,
) : InternalState() {

    private class Data {
        val listeners = mutableSetOf<IState.Listener>()

        val states = mutableSetOf<InternalState>()
        var initialState: InternalState? = null

        /**
         * In [ChildMode.EXCLUSIVE] is null if there are no child states, or if a state is not active.
         * In [ChildMode.PARALLEL] it is always null.
         */
        var currentState: InternalState? = null
        val transitions = mutableSetOf<Transition<*>>()
        var isActive = false
        var isFinished = false
        var internalParent: InternalState? = null
        var metaInfo: MetaInfo? = null
        var payload: Any? = null
    }

    /**
     * Encapsulates all mutable fields
     */
    private var data = Data()

    override val listeners: Collection<IState.Listener> get() = data.listeners

    override val states: Set<IState> get() = data.states

    override val initialState get() = data.initialState

    override val machine get() = if (this is StateMachine) this else requireInternalParent().machine

    override val transitions: Set<Transition<*>> get() = data.transitions

    override val isActive get() = data.isActive
    override val isFinished get() = data.isFinished

    override val internalParent get() = data.internalParent
    override var metaInfo: MetaInfo?
        get() = data.metaInfo
        set(value) {
            if (machineOrNull()?.isRunning == true) error("Can not change metaInfo after state machine started")
            data.metaInfo = value
        }

    override var payload: Any?
        get() = data.payload
        set(value) {
            data.payload = value
        }


    override fun setParent(parent: InternalState) {
        check(parent !== data.internalParent) { "$parent is already a parent of $this" }
        if (data.internalParent != null)
            onStateReuseDetected()
        data.internalParent = parent
    }

    private fun onStateReuseDetected() {
        val machine = machine
        if (machine.creationArguments.autoDestroyOnStatesReuse)
            machine.destroyBlocking()
        else
            error("State $this is already used in another machine instance")
    }

    override fun <L : IState.Listener> addListener(listener: L): L {
        require(data.listeners.add(listener)) { "$listener is already added" }
        return listener
    }

    override fun removeListener(listener: IState.Listener) {
        data.listeners.remove(listener)
    }

    override fun <S : IState> addState(state: S): S {
        if (machineOrNull()?.isRunning == true) error("Can not add state after state machine started")
        if (childMode == PARALLEL) {
            require(state !is IFinalState) { "Can not add IFinalState in parallel child mode" }
            require(state !is PseudoState) { "Can not add PseudoState in parallel child mode" }
        }

        state.name?.let {
            require(findState(it, recursive = false) == null) { "State with name $it already exists" }
        }

        state as InternalState
        require(data.states.add(state)) { "$state already added" }
        state.setParent(this)
        return state
    }

    override fun setInitialState(state: IState) {
        require(states.contains(state)) { "$state is not part of $this machine, use addState() first" }
        check(childMode == EXCLUSIVE) { "Can not set initial state in parallel child mode" }
        if (machineOrNull()?.isRunning == true) error("Can not change initial state after state machine started")

        data.initialState = state as InternalState
    }

    override fun <E : Event> addTransition(transition: Transition<E>): Transition<E> {
        if (machineOrNull()?.isRunning == true) error("Can not add transition after state machine started")
        data.transitions += transition
        return transition
    }

    override fun toString() = "${this::class.simpleName}(${if (name != null) "$name" else "$${hashCode()}"})"

    override fun asState() = this

    protected open suspend fun onDoEnter(transitionParams: TransitionParams<*>) = Unit // default empty
    protected open suspend fun onDoExit(transitionParams: TransitionParams<*>) = Unit // default empty

    override suspend fun doEnter(transitionParams: TransitionParams<*>) {
        if (!isActive) {
            val machine = machine
            if (parent != null) machine.log { "Parent $parent entering child $this" }
            data.isActive = true
            onDoEnter(transitionParams)
            stateNotify { onEntry(transitionParams) }
            machine.machineNotify { onStateEntry(this@BaseStateImpl, transitionParams) }
        }
    }

    override suspend fun doExit(transitionParams: TransitionParams<*>) {
        if (isActive) {
            log { "Exiting $this" }
            onDoExit(transitionParams)
            data.currentState = null
            data.isFinished = false
            data.isActive = false
            stateNotify { onExit(transitionParams) }
            machine.machineNotify { onStateExit(this@BaseStateImpl, transitionParams) }
        }
    }

    override suspend fun afterChildFinished(finishedChild: InternalState, transitionParams: TransitionParams<*>) {
        if (childMode == PARALLEL && states.all { it.isFinished }) {
            data.isFinished = true
            notifyStateFinish(finishedChild, transitionParams)
        }
    }

    override suspend fun <E : Event> recursiveFindUniqueResolvedTransition(
        eventAndArgument: EventAndArgument<E>
    ): ResolvedTransition<E>? {
        val resolvedTransitions = getCurrentStates()
            .filter { it !is StateMachine } // exclude nested machines
            .mapNotNull { it.recursiveFindUniqueResolvedTransition(eventAndArgument) }
            .ifEmpty { listOfNotNull(findUniqueResolvedTransition(eventAndArgument)) } // allow transition override
        return if (!machine.creationArguments.doNotThrowOnMultipleTransitionsMatch) {
            check(resolvedTransitions.size <= 1) {
                "Multiple transitions match ${eventAndArgument.event}, $resolvedTransitions in $this"
            }
            resolvedTransitions.singleOrNull()
        } else {
            resolvedTransitions.firstOrNull()
        }
    }

    override suspend fun recursiveEnterInitialStates(transitionParams: TransitionParams<*>) {
        if (states.isEmpty()) return

        when (childMode) {
            EXCLUSIVE -> {
                val initialState = requireInitialState() as InternalState
                setCurrentState(initialState, transitionParams)
                if (initialState !is StateMachine)  // inner state machine manages its internal state by its own
                    initialState.recursiveEnterInitialStates(transitionParams)
            }
            PARALLEL -> data.states.forEach {
                handleStateEntry(it, transitionParams)
                if (it !is StateMachine) // inner state machine manages its internal state by its own
                    it.recursiveEnterInitialStates(transitionParams)
            }
        }
    }

    override suspend fun recursiveEnterStatePath(
        path: ListIterator<InternalState>,
        transitionParams: TransitionParams<*>
    ) {
        if (!path.hasPrevious()) {
            recursiveEnterInitialStates(transitionParams)
        } else {
            val state = path.previous()
            when (childMode) {
                EXCLUSIVE -> {
                    setCurrentState(state, transitionParams)
                    if (state !is StateMachine) // inner state machine manages its internal state by its own
                        state.recursiveEnterStatePath(path, transitionParams)
                }
                PARALLEL -> data.states.forEach {
                    handleStateEntry(it, transitionParams)
                    if (it !is StateMachine) { // inner state machine manages its internal state by its own
                        if (it === state)
                            it.recursiveEnterStatePath(path, transitionParams)
                        else
                            recursiveEnterInitialStates(transitionParams)
                    }
                }
            }
        }
    }

    override suspend fun recursiveEnterStatePath(
        pathHead: PathNode,
        transitionParams: TransitionParams<*>
    ) {
        if (pathHead.children.isEmpty()) {
            recursiveEnterInitialStates(transitionParams)
        } else {
            when (childMode) {
                EXCLUSIVE -> {
                    val exclusivePath = pathHead.children.singleOrNull() ?: error(
                        "Looks that you have specified multiple targets for exclusive state, which is not correct, " +
                                "calculated targets: ${pathHead.children.joinToString { it.state.toString() }}, " +
                                "parent: $this"
                    )
                    val state = exclusivePath.state as InternalState
                    setCurrentState(state, transitionParams)
                    if (state !is StateMachine) // inner state machine manages its internal state by its own
                        state.recursiveEnterStatePath(exclusivePath, transitionParams)
                }
                PARALLEL -> data.states.forEach { childState ->
                    val paths = pathHead.children
                    handleStateEntry(childState, transitionParams)
                    if (childState !is StateMachine) { // inner state machine manages its internal state by its own
                        val regionPath = paths.find { it.state === childState }
                        if (regionPath != null) {
                            childState.recursiveEnterStatePath(
                                regionPath,
                                transitionParams.repackForRegion(regionPath.requireFirstLeaf().state as IState)
                            )
                        } else {
                            childState.recursiveEnterInitialStates(transitionParams)
                        }
                    }
                }
            }
        }
    }

    override suspend fun recursiveExit(transitionParams: TransitionParams<*>) {
        getCurrentStates().forEachState { it.recursiveExit(transitionParams) }
        doExit(transitionParams)
    }

    override suspend fun recursiveStop() {
        data.currentState = null
        data.isActive = false
        data.isFinished = false
        onStopped()
        data.states.forEachState { it.recursiveStop() }
    }

    override suspend fun recursiveAfterTransitionComplete(transitionParams: TransitionParams<*>) {
        data.states.forEachState { it.recursiveAfterTransitionComplete(transitionParams) }
    }

    override fun getCurrentStates() = when (childMode) {
        EXCLUSIVE -> listOfNotNull(data.currentState)
        PARALLEL -> data.states.toList()
    }

    private suspend fun setCurrentState(state: InternalState, transitionParams: TransitionParams<*>) {
        require(childMode == EXCLUSIVE) { "Cannot set current state in child mode $childMode" }
        require(states.contains(state)) { "$state is not a child of $this" }

        if (data.currentState === state && transitionParams.transition.type != EXTERNAL) return
        data.currentState?.recursiveExit(transitionParams)
        data.currentState = state

        data.states.forEachState { it.onParentCurrentStateChanged(state) }
        handleStateEntry(state, transitionParams)
    }

    override suspend fun cleanup() {
        data = Data()
        onCleanup()
    }

    private suspend fun handleStateEntry(state: InternalState, transitionParams: TransitionParams<*>) {
        val finished = when (childMode) {
            EXCLUSIVE -> state is IFinalState
            PARALLEL -> states.all { it.isFinished }
        }
        data.isFinished = finished

        state.doEnter(transitionParams)

        if (finished) {
            notifyStateFinish(state, transitionParams)
            internalParent?.afterChildFinished(this, transitionParams)
        }
    }

    private suspend fun notifyStateFinish(state: InternalState, transitionParams: TransitionParams<*>) {
        log { "$this finished" }
        stateNotify { onFinished(transitionParams) }
        machine.machineNotify { onStateFinished(this@BaseStateImpl, transitionParams) }
        // there is no sense to send event on state machine finish as it stops processing events in this case
        if (this !is StateMachine)
            machine.processEvent(makeFinishedEvent(state))
    }

    private fun makeFinishedEvent(state: InternalState): FinishedEvent {
        // check for both interfaces as client is not forced to use FinalDataState
        return if (childMode == EXCLUSIVE && state is DataState<*> && state is IFinalState)
            FinishedEvent(this, state.data)
        else
            FinishedEvent(this)
    }

    internal suspend fun switchToTargetStates(
        targetStates: Set<InternalState>,
        fromState: InternalState,
        transitionParams: TransitionParams<*>
    ) {
        when {
            targetStates.isEmpty() -> return
            targetStates.size == 1 -> {
                @Suppress("UNCHECKED_CAST")
                val path = fromState.findPathFromTargetToLca(
                    targetStates.single(),
                    transitionParams.transition.type == EXTERNAL
                ) as List<InternalState>
                val iterator = path.listIterator(path.size)
                iterator.previous().recursiveEnterStatePath(iterator, transitionParams)
            }
            else -> {
                val pathHead = fromState.findTreePathFromTargetsToLca(
                    targetStates,
                    transitionParams.transition.type == EXTERNAL
                )
                val state = pathHead.state as InternalState
                state.recursiveEnterStatePath(pathHead, transitionParams)
            }
        }
    }
}

private fun TransitionParams<*>.repackForRegion(regionTargetState: IState): TransitionParams<*> {
    return if (direction.targetState === regionTargetState) {
        this
    } else {
        copy(direction = TargetState(direction.targetStates, regionTargetState))
    }
}