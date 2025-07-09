package com.example.graphapp.data.schema

import com.example.graphapp.data.GraphRepository
import com.example.graphapp.data.VectorRepository
import com.example.graphapp.data.local.NodeEntity
import kotlin.io.path.Path
import kotlin.math.ln

private const val DECAY_FACTOR = 0.8f
private const val ITERATIONS = 5

/* -------------------------------------------------
  Helper to compute Weighted Similarity between nodes
------------------------------------------------- */
fun computeWeightedSim(
    targetId: Long,
    candidateId: Long,
    repository: VectorRepository,
    similarityMatrix: Map<Pair<Long, Long>, Float>
): Float {

    val occurrenceCounts = repository.getAllNodeFrequencies()

    val rawSim = similarityMatrix.getOrDefault(targetId to candidateId, 0f)
    val freqFactor = ln(1f + (occurrenceCounts[candidateId] ?: 0)).toFloat()
    val adjustedSim = rawSim * freqFactor

    return adjustedSim
}

/* -------------------------------------------------
  Helpers to initialise Semantic Similarity Matrix
------------------------------------------------- */
fun getPropertyEmbeddings(
    nodeId: Long,
    repository: VectorRepository
): Map<String, FloatArray?> {

    val neighbourNodes = repository.getNeighborsOfNodeById(nodeId)
    val embeddings = mutableMapOf<String, FloatArray?>()

    for (node in neighbourNodes) {
        if (node.type in GraphSchema.propertyNodes) {
            embeddings[node.type] = node.embedding
        }
    }

    return embeddings
}

fun computeSemanticSimilarity(
    nodeId1: Long,
    nodeId2: Long,
    repository: VectorRepository,
    threshold: Float = 0.5f
): Float {
    val e1 = getPropertyEmbeddings(nodeId1, repository)
    val e2 = getPropertyEmbeddings(nodeId2, repository)

    val similarities = mutableListOf<Float>()

    for (prop in GraphSchema.propertyNodes) {
        val v1 = e1[prop]
        val v2 = e2[prop]
        if (v1 == null || v2 == null) continue

        val similarity = repository.cosineDistance(v1, v2)

        if (similarity < threshold) { return 0f }

        similarities.add(similarity)
    }

    if (similarities.isEmpty()) return 0f

    return similarities.average().toFloat()
}

fun initialiseSemanticSimilarityMatrix(
    repository: VectorRepository,
    threshold: Float = 0.5f
): Map<Pair<Long, Long>, Float> {

    val allNodes = repository.getAllNodes()
    val nodeIds = allNodes.map { it.id }

    val sim = mutableMapOf<Pair<Long, Long>, Float>()
    for (i in nodeIds) {
        for (j in nodeIds) {
            val score = if (i == j) {
                1f
            } else {
                computeSemanticSimilarity(i, j, repository, threshold)
            }
            sim[i to j] = score
        }
    }
    return sim
}


/* -------------------------------------------------
  Helper to initialise SimRank Similarity Matrix
------------------------------------------------- */
fun initialiseSimRankSimilarityMatrix(
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

/* -------------------------------------------------
  Helper to initialise Common Neighbours Similarity Matrix
------------------------------------------------- */
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









