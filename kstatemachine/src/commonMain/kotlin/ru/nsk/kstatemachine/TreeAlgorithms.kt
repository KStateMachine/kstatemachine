package ru.nsk.kstatemachine

/**
 * @return Path from [targetState] to the lowest common ancestor (lca).
 * Path order: Lca and its parent if [addLcaParent] is true are last elements in resulting list.
 * This path always contains at lest one element - lca
 */
internal fun InternalState.findPathFromTargetToLca(
    targetState: InternalState,
    addLcaParent: Boolean,
): List<InternalState> {
    var thisNode = this
    var targetNode = targetState
    var thisDepth = thisNode.findDepth()
    var targetDepth = targetState.findDepth()
    val targetPath = mutableListOf<InternalState>()

    while (thisDepth != targetDepth) {
        if (thisDepth > targetDepth) {
            thisNode = thisNode.requireInternalParent()
            thisDepth--
        } else {
            targetPath.add(targetNode)

            targetNode = targetNode.requireInternalParent()
            targetDepth--
        }
    }

    while (thisNode !== targetNode) {
        thisNode = thisNode.requireInternalParent()

        targetPath.add(targetNode)
        targetNode = targetNode.requireInternalParent()
    }

    targetPath.add(thisNode) // add lca
    if (addLcaParent) thisNode.internalParent?.let { targetPath.add(it) }
    return targetPath
}

/**
 * Builds a tree representing a path from [targetStates] to theirs lca with implicit receiver
 */
internal fun InternalState.findTreePathFromTargetsToLca(
    targetStates: Set<InternalState>,
    addLcaParent: Boolean,
): PathNode {
    var thisNode = this
    var targetNode = targetState
    var thisDepth = thisNode.findDepth()
    var targetDepth = targetState.findDepth()
    val targetPath = mutableListOf<InternalState>()

    while (thisDepth != targetDepth) {
        if (thisDepth > targetDepth) {
            thisNode = thisNode.requireInternalParent()
            thisDepth--
        } else {
            targetPath.add(targetNode)

            targetNode = targetNode.requireInternalParent()
            targetDepth--
        }
    }

    while (thisNode !== targetNode) {
        thisNode = thisNode.requireInternalParent()

        targetPath.add(targetNode)
        targetNode = targetNode.requireInternalParent()
    }

    targetPath.add(thisNode) // add lca
    if (addLcaParent) thisNode.internalParent?.let { targetPath.add(it) }
    return targetPath
}

internal fun findLca(states: Set<InternalState>): InternalState {
    require(states.isNotEmpty()) { "States set is empty" }

    val stateDepths = mutableListOf<Pair<InternalState, Int>>()
    states.mapTo(stateDepths) { it to it.findDepth() }

    val minDepth = stateDepths.minOf { it.second }

    val balancedStates = mutableListOf<InternalState>()
    stateDepths.mapTo(balancedStates) {
        var (state, depth) = it
        while (minDepth != depth) {
            state = state.requireInternalParent()
            depth--
        }
        state
    }

    fun List<InternalState>.areAllElementsSame(): Boolean {
        val first = first()
        return all { it === first }
    }

    while (!balancedStates.areAllElementsSame()) {
        balancedStates.forEachIndexed { index, state ->
            balancedStates[index] = state.requireInternalParent()
        }
    }
    return balancedStates.first()
}

private fun IState.findDepth(): Int {
    var depth = 0
    var parent = this.parent
    while (parent != null) {
        depth++
        parent = parent.parent
    }
    return depth
}