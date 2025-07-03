package com.example.graphapp.data.schema

import com.example.graphapp.data.GraphRepository

private const val DECAY_FACTOR = 0.8f
private const val ITERATIONS = 5

fun initialiseSimilarityMatrix(
    repository: GraphRepository
): Map<Pair<Long, Long>, Float> {
    // Get all nodes
    val allNodes = repository.getAllNodes()
    val nodeIds = allNodes.map { it.id }

    // Get all edges
    val edges = repository.getAllEdges()
    val neighborMap = mutableMapOf<Long, MutableSet<Long>>()
    for (edge in edges) {
        neighborMap.getOrPut(edge.fromNode) { mutableSetOf() }.add(edge.toNode)
        neighborMap.getOrPut(edge.toNode) { mutableSetOf() }.add(edge.fromNode)
    }

    // Compute SimRank similarity
    val sim = mutableMapOf<Pair<Long, Long>, Float>()
    for (i in nodeIds) {
        for (j in nodeIds) {
            sim[i to j] = if (i == j) 1f else 0f
        }
    }

    repeat(ITERATIONS) {
        val newSim = mutableMapOf<Pair<Long, Long>, Float>()
        for (i in nodeIds) {
            for (j in nodeIds) {
                if (i == j) {
                    newSim[i to j] = 1f
                } else {
                    val neighborsI = neighborMap[i].orEmpty()
                    val neighborsJ = neighborMap[j].orEmpty()
                    if (neighborsI.isEmpty() || neighborsJ.isEmpty()) {
                        newSim[i to j] = 0f
                    } else {
                        val sum = neighborsI.sumOf { ni ->
                            neighborsJ.sumOf { nj ->
                                sim.getOrDefault(ni to nj, 0f).toDouble()
                            }
                        }
                        newSim[i to j] = (DECAY_FACTOR * (sum / (neighborsI.size * neighborsJ.size))).toFloat()
                    }
                }
            }
        }
        sim.clear()
        sim.putAll(newSim)
    }

    return sim
}

fun explainSimRankContributors(
    nodeA: Long,
    nodeB: Long,
    repository: GraphRepository,
    simMatrix: Map<Pair<Long, Long>, Float>
): List<Triple<Long, Long, Float>> {

    val inNeighborsA = repository.getNeighborsOfNodeById(nodeA)
    val inNeighborsB = repository.getNeighborsOfNodeById(nodeB)

    val contributions = mutableListOf<Triple<Long, Long, Float>>()

    for (i in inNeighborsA) {
        for (j in inNeighborsB) {
            val sim = simMatrix.getOrDefault(i.id to j.id, 0f)
            if (sim > 0f) {
                contributions.add(Triple(i.id, j.id, sim))
            }
        }
    }

    // Sort by similarity descending
    return contributions.sortedByDescending { it.third }
}
