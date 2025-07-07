package com.example.graphapp.data.schema

import com.example.graphapp.data.GraphRepository
import kotlin.math.ln

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

fun computeWeightedSim(
    targetId: Long,
    candidateId: Long,
    repository: GraphRepository
): Float {
    val simMatrix = initialiseCommonNeighborsSimilarityMatrix(repository)
    val occurrenceCounts = repository.selectAllFreq()

    val rawSim = simMatrix.getOrDefault(targetId to candidateId, 0f)
    val freqFactor = ln(1f + (occurrenceCounts[candidateId] ?: 0)).toFloat()
    val adjustedSim = rawSim * freqFactor

    return adjustedSim
}

fun initialiseCommonNeighborsSimilarityMatrix(
    repository: GraphRepository
): Map<Pair<Long, Long>, Float> {
    // 1. Get all nodes
    val allNodes = repository.getAllNodes()
    val nodeIds = allNodes.map { it.id }

    // 2. Get all edges and build adjacency list
    val edges = repository.getAllEdges()
    val neighborMap = mutableMapOf<Long, MutableSet<Long>>()
    for (edge in edges) {
        neighborMap.getOrPut(edge.fromNode) { mutableSetOf() }.add(edge.toNode)
        neighborMap.getOrPut(edge.toNode) { mutableSetOf() }.add(edge.fromNode)
    }

    // 3. Compute Common Neighbors similarity matrix
    val sim = mutableMapOf<Pair<Long, Long>, Float>()

    for (i in nodeIds) {
        for (j in nodeIds) {
            if (i == j) {
                sim[i to j] = 1f
            } else {
                val neighborsI = neighborMap[i].orEmpty()
                val neighborsJ = neighborMap[j].orEmpty()

                if (neighborsI.isEmpty() || neighborsJ.isEmpty()) {
                    sim[i to j] = 0f
                } else {
                    val intersectionSize = neighborsI.intersect(neighborsJ).size
                    val unionSize = neighborsI.union(neighborsJ).size

                    val score = if (unionSize == 0) 0f else (intersectionSize.toFloat() / unionSize)
                    sim[i to j] = score
                }
            }
        }
    }

    return sim
}




