package ru.nsk.kstatemachine

/**
 * @return Path from [targetState] to the lowest common ancestor (lca).
 * Path order: Lca and its parent if [addLcaParent] is true are last elements in resulting list.
 * This path always contains at lest one element - lca
 */
internal fun InternalNode.findPathFromTargetToLca(
    targetState: InternalNode,
    addLcaParent: Boolean,
): List<InternalNode> {
    var thisNode = this
    var targetNode = targetState
    var thisDepth = thisNode.findDepth()
    var targetDepth = targetState.findDepth()
    val targetPath = mutableListOf<InternalNode>()

    while (thisDepth != targetDepth) {
        if (thisDepth > targetDepth) {
            thisNode = thisNode.requireParentNode()
            thisDepth--
        } else {
            targetPath.add(targetNode)

            targetNode = targetNode.requireParentNode()
            targetDepth--
        }
    }

    while (thisNode !== targetNode) {
        thisNode = thisNode.requireParentNode()

        targetPath.add(targetNode)
        targetNode = targetNode.requireParentNode()
    }

    targetPath.add(thisNode) // add lca
    if (addLcaParent) thisNode.internalParent?.let { targetPath.add(it) }
    return targetPath
}

internal data class PathNode(
    val state: InternalNode,
    val children: MutableSet<PathNode>,
)

/**
 * Builds a tree representing a path from [targetStates] to theirs lca with implicit receiver
 */
internal fun InternalNode.findTreePathFromTargetsToLca(
    targetStates: Set<InternalNode>,
    addLcaParent: Boolean,
): PathNode {
    require(targetStates.isNotEmpty()) { "States set is empty" }

    data class StatePointer(
        var path: PathNode,
        var depth: Int,
        val ignore: Boolean = false
    )

    val statePointers = mutableListOf<StatePointer>()
    targetStates.mapTo(statePointers) { StatePointer(PathNode(it, mutableSetOf()), it.findDepth()) }
    statePointers += StatePointer(PathNode(this, mutableSetOf()), this.findDepth(), true)

    do {
        val maxDepth = statePointers.maxOf { it.depth }
        statePointers.filter { it.depth == maxDepth }.forEach {
            it.path = PathNode(it.path.state.requireParentNode(), mutableSetOf(it.path))
            it.depth--
        }

        val deepestStatePointers = statePointers.filter { it.depth == maxDepth - 1 }
        val groups = deepestStatePointers.groupBy { it.path.state }
        for (entry in groups) {
            // merge branches into leading one and remove merged
            val leadBranchIndex = entry.value.indexOfFirst { !it.ignore }
            if (leadBranchIndex == -1) continue
            entry.value.forEachIndexed { index, statePointer ->
                if (index != leadBranchIndex) {
                    if (!statePointer.ignore)
                        entry.value[leadBranchIndex].path.children.addAll(statePointer.path.children)
                    statePointers.remove(statePointer)
                }
            }
        }
    } while (statePointers.size > 1)

    val path = statePointers.single().path
    if (addLcaParent)
        path.state.internalParent?.let {
            return PathNode(it, mutableSetOf(path))
        }
    return path
}

internal fun findLca(states: Set<InternalNode>): InternalNode {
    require(states.isNotEmpty()) { "States set is empty" }

    val stateDepths = mutableListOf<Pair<InternalNode, Int>>()
    states.mapTo(stateDepths) { it to it.findDepth() }

    val minDepth = stateDepths.minOf { it.second }

    val balancedStates = mutableListOf<InternalNode>()
    stateDepths.mapTo(balancedStates) {
        var (state, depth) = it
        while (minDepth != depth) {
            state = state.requireParentNode()
            depth--
        }
        state
    }

    fun List<InternalNode>.areAllElementsSame(): Boolean {
        val first = first()
        return all { it === first }
    }

    while (!balancedStates.areAllElementsSame()) {
        balancedStates.forEachIndexed { index, state ->
            balancedStates[index] = state.requireParentNode()
        }
    }
    return balancedStates.first()
}

internal fun InternalNode.findDepth(): Int {
    var depth = 0
    var parent = this.internalParent
    while (parent != null) {
        depth++
        parent = parent.internalParent
    }
    return depth
}