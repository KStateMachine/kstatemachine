package ru.nsk.kstatemachine

import ru.nsk.kstatemachine.visitors.CoVisitor
import ru.nsk.kstatemachine.visitors.GetActiveStatesVisitor
import ru.nsk.kstatemachine.visitors.Visitor
import ru.nsk.kstatemachine.visitors.VisitorAcceptor
import kotlin.reflect.KClass

/**
 * Base interface for all kind of states.
 * Many additional methods implemented like extensions to keep this interface clean and minimalistic.
 */
@StateMachineDslMarker
interface IState : TransitionStateApi, VisitorAcceptor {
    val name: String?
    val states: Set<IState>
    val initialState: IState?
    val parent: IState?
    val machine: StateMachine
    val isActive: Boolean
    val isFinished: Boolean
    val listeners: Collection<Listener>
    val childMode: ChildMode

    fun <L : Listener> addListener(listener: L): L
    fun removeListener(listener: Listener)

    fun <S : IState> addState(state: S, init: StateBlock<S>? = null): S

    /**
     * Initial child state is required if child mode is [ChildMode.EXCLUSIVE] and a state has children
     */
    fun setInitialState(state: IState)

    override suspend fun accept(visitor: CoVisitor) = machine.coroutineAbstraction.withContext {
        visitor.visit(this)
    }

    override fun accept(visitor: Visitor) = visitor.visit(this)

    /**
     * Called when machine is stopped, to perform cleanup steps.
     */
    suspend fun onStopped() = Unit

    /**
     * Called when state is sequentially used on multiple machine instances, to perform cleanup steps.
     */
    suspend fun onCleanup() = Unit

    interface Listener {
        suspend fun onEntry(transitionParams: TransitionParams<*>) = Unit
        suspend fun onExit(transitionParams: TransitionParams<*>) = Unit

        /**
         * If child mode is [ChildMode.EXCLUSIVE] notifies that child [IFinalState] is entered.
         * If child mode is [ChildMode.PARALLEL] notifies that all children has finished.
         */
        suspend fun onFinished(transitionParams: TransitionParams<*>) = Unit
    }
}

enum class ChildMode { EXCLUSIVE, PARALLEL }
enum class HistoryType {
    /** Records only immediate child states */
    SHALLOW,

    /** Records nested states also */
    DEEP
}

/**
 * Simple state in most cases equal to [IState].
 * Using this interface explicitly says that the state does not have a data field that is used by typesafe transitions.
 * Helps to break compilation if a user specifies [DataState] as a target of non-data transition.
 */
interface State : IState

/**
 * State which holds data while it is active
 */
interface DataState<D : Any> : IState {
    val defaultData: D?

    /**
     * This property might be accessed only while this state is active
     */
    val data: D

    /**
     * Similar to [data] but its value is not cleared when state finishes.
     * If state was not entered this property falls back to [defaultData] if it was specified
     */
    val lastData: D
}

/**
 * Marker interface. When [StateMachine] enters this state it finishes and does not accept events anymore.
 * It is possible to use this interface to mark final state directly instead of subclassing [DefaultFinalState]
 */
interface IFinalState : IState
interface FinalState : IFinalState, State
interface FinalDataState<D : Any> : IFinalState, DataState<D>

/**
 * Pseudo state is a state that machine passes automatically without explicit event. It cannot be active.
 */
interface PseudoState : State

interface RedirectPseudoState : PseudoState {
    suspend fun resolveTargetState(eventAndArgument: EventAndArgument<*>): IState
}

/**
 * Pseudo-state that represents the child state that the parent state was in the last time before exited.
 */
interface HistoryState : PseudoState {
    val historyType: HistoryType

    /** Initial parent state if was not set explicitly */
    val defaultState: IState
    val storedState: IState
}

typealias StateBlock<S> = S.() -> Unit

/**
 * Set of states that the state is currently in. Including state itself if [selfIncluding] is true.
 * Internal states of nested machines are not included.
 */
fun IState.activeStates(selfIncluding: Boolean = false): Set<IState> {
    val visitor = GetActiveStatesVisitor(selfIncluding)
    accept(visitor)
    return visitor.activeStates
}

/**
 * Get state by name. This might be used to start listening to state after state machine setup.
 */
fun IState.findState(name: String, recursive: Boolean = true): IState? {
    val result = states.find { it.name == name }

    if (!recursive || result != null)
        return result

    return states.firstNotNullOfOrNull {
        if (it is StateMachine) // do not go into nested state machines
            null
        else
            it.findState(name, true)
    }
}

fun IState.requireState(name: String, recursive: Boolean = true) =
    requireNotNull(findState(name, recursive)) { "State $name not found" }

/**
 * Find state by type. Search by type is suitable when using own state subclasses that usually do not have a name.
 * Only on state should match the type or exception will be thrown.
 */
inline fun <reified S : IState> IState.findState(recursive: Boolean = true) = findState(S::class, recursive)

/**
 * For internal use. Workaround that Kotlin does not support recursive inline functions.
 */
@Suppress("UNCHECKED_CAST")
fun <S : IState> IState.findState(`class`: KClass<S>, recursive: Boolean = true): S? {
    fun requireSingleOrEmpty(collection: Collection<*>) = require(collection.size <= 1) {
        "More than one state matches ${`class`.simpleName}"
    }

    val filtered = states.filter { `class`.isInstance(it) }
    requireSingleOrEmpty(filtered)

    if (!recursive) return filtered.singleOrNull() as S?

    val nestedFiltered = states.mapNotNull { it.findState(`class`, true) }
    requireSingleOrEmpty(nestedFiltered)

    val allFiltered = filtered + nestedFiltered
    requireSingleOrEmpty(allFiltered)

    return allFiltered.singleOrNull() as S?
}

/**
 * Require state by type
 */
inline fun <reified S : IState> IState.requireState(recursive: Boolean = true) =
    requireNotNull(findState<S>(recursive)) { "State ${S::class.simpleName} not found" }

operator fun <S : IState> S.invoke(block: StateBlock<S>) = block()

fun IState.machineOrNull(): StateMachine? = if (this is StateMachine) this else parent?.machineOrNull()

/**
 * @param name is optional and is useful for getting state instance after state machine setup
 * with [IState.findState] and for debugging.
 */
fun IState.state(
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    init: StateBlock<State>? = null
) = addState(DefaultState(name, childMode), init)

inline fun <reified D : Any> IState.dataState(
    name: String? = null,
    defaultData: D? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
    noinline init: StateBlock<DataState<D>>? = null
) = addState(defaultDataState(name, defaultData, childMode, dataExtractor), init)

/**
 * A shortcut for [state] and [IState.setInitialState] calls
 */
fun IState.initialState(
    name: String? = null,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    init: StateBlock<State>? = null
) = addInitialState(DefaultState(name, childMode), init)

/**
 * @param defaultData is necessary for initial [DataState]
 */
inline fun <reified D : Any> IState.initialDataState(
    name: String? = null,
    defaultData: D,
    childMode: ChildMode = ChildMode.EXCLUSIVE,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
    noinline init: StateBlock<DataState<D>>? = null
) = addInitialState(defaultDataState(name, defaultData, childMode, dataExtractor), init)

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
    addFinalState(DefaultFinalState(name), init)

inline fun <reified D : Any> IState.finalDataState(
    name: String? = null,
    defaultData: D? = null,
    dataExtractor: DataExtractor<D> = defaultDataExtractor(),
    noinline init: StateBlock<FinalDataState<D>>? = null
) = addFinalState(defaultFinalDataState(name, defaultData, dataExtractor), init)

fun IState.choiceState(name: String? = null, choiceAction: suspend EventAndArgument<*>.() -> State) =
    addState(DefaultChoiceState(name, choiceAction))

fun IState.historyState(
    name: String? = null,
    defaultState: IState? = null,
    historyType: HistoryType = HistoryType.SHALLOW
) = addState(DefaultHistoryState(name, defaultState, historyType))