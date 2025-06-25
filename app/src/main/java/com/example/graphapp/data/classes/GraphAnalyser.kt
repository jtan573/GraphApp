package com.example.graphapp.data.classes

object GraphAnalyser {
    fun commonNeighborsScore(
        nodeA: Long,
        nodeB: Long,
        neighborMap: Map<Long, Set<Long>>
    ): Int {
        val neighborsA = neighborMap[nodeA] ?: emptySet()
        val neighborsB = neighborMap[nodeB] ?: emptySet()
        return neighborsA.intersect(neighborsB).size
    }
}