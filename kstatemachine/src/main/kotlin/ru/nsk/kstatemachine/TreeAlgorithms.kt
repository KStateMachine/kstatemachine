package ru.nsk.kstatemachine

internal object TreeAlgorithms {
    /**
     * @return lca and a path from lca to [targetState]
     */
    fun InternalState.findLowestCommonAncestor(targetState: InternalState):
            Pair<InternalState, MutableList<InternalState>> {
        var thisNode = this
        var targetNode = targetState
        var thisDepth = thisNode.findDepth()
        var targetDepth = targetState.findDepth()
        val targetPath = mutableListOf<InternalState>()

        while (thisDepth != targetDepth) {
            if (thisDepth > targetDepth) {
                thisNode = thisNode.parent
                thisDepth--
            } else {
                targetPath.add(targetNode)

                targetNode = targetNode.parent
                targetDepth--
            }
        }

        while (thisNode !== targetNode) {
            thisNode = thisNode.parent

            targetPath.add(targetNode)
            targetNode = targetNode.parent
        }

        return thisNode to targetPath
    }

    private fun State.findDepth(): Int {
        var currentState = this
        var depth = 0
        while (currentState !is StateMachine) {
            depth++
            currentState = currentState.parent
        }
        return depth
    }
}