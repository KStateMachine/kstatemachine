package ru.nsk.kstatemachine

internal object TreeAlgorithms {
    /**
     * @return Path from [targetState] to the lowest common ancestor (lca).
     * This path always contains at lest one element - lca which is last.
     */
    fun InternalState.findPathFromTargetToLca(targetState: InternalState): MutableList<InternalState> {
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
        return targetPath
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
}
