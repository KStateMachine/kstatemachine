package ru.nsk.kstatemachine.transition

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.event.EventMatcher
import ru.nsk.kstatemachine.metainfo.MetaInfo
import ru.nsk.kstatemachine.state.IState
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.transition.TransitionType.EXTERNAL
import ru.nsk.kstatemachine.transition.TransitionType.LOCAL
import ru.nsk.kstatemachine.visitors.CoVisitor
import ru.nsk.kstatemachine.visitors.Visitor
import ru.nsk.kstatemachine.visitors.VisitorAcceptor

/**
 * Represent a transition between states, which gets triggered when specified [Event] is posted to [StateMachine]
 */
interface Transition<E : Event> : VisitorAcceptor {
    val name: String?
    val eventMatcher: EventMatcher<E>
    val sourceState: IState
    val type: TransitionType
    /**
     * This parameter may be used to pass arbitrary data with a transition to targetState.
     * This argument may be set from transition listener. Such transition must have only one listener
     * that sets the argument.
     */
    var argument: Any?

    val listeners: Collection<Listener>
    val metaInfo: MetaInfo?

    fun <L : Listener> addListener(listener: L): L
    fun removeListener(listener: Listener)

    /**
     * Checks if the [event] matches this [Transition]
     */
    suspend fun isMatchingEvent(event: Event): Boolean

    override suspend fun accept(visitor: CoVisitor) = sourceState.machine.coroutineAbstraction.withContext {
        visitor.visit(this)
    }

    override fun accept(visitor: Visitor) = visitor.visit(this)

    interface Listener {
        /**
         * Called when transition is triggered/performed
         */
        suspend fun onTriggered(transitionParams: TransitionParams<*>) = Unit

        /**
         * Triggered after transition is complete and provides set of current active states
         */
        suspend fun onComplete(transitionParams: TransitionParams<*>, activeStates: Set<IState>) = Unit
    }
}

/**
 * Most of the cases [EXTERNAL] and [LOCAL] transition are functionally equivalent except in cases where transition
 * is happening between super and sub-states. Local transition doesn't cause exit and entry to source state if
 * target state is a sub-state of a source state.
 * Other way around, local transition doesn't cause exit and entry to target state if target is a superstate
 * of a source state.
 */
enum class TransitionType {
    /** Default */
    LOCAL,
    EXTERNAL
}