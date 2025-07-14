package com.example.graphapp.data.schema

import android.util.Log
import com.example.graphapp.data.GraphRepository
import com.example.graphapp.data.VectorRepository
import com.example.graphapp.data.local.NodeEntity
import com.example.graphapp.data.schema.GraphSchema.SchemaKeyNodes
import com.example.graphapp.data.schema.GraphSchema.SchemaPropertyNodes
import com.example.graphdb.Node
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
    val freqFactor = ln(1f + (occurrenceCounts[candidateId] ?: 1)).toFloat()

//    Log.d("CHECK SIM", "$rawSim to $freqFactor")
    val adjustedSim = rawSim * freqFactor

    return adjustedSim
}

/* -------------------------------------------------
  Helpers to initialise Semantic Similarity Matrix
------------------------------------------------- */
fun getPropertyEmbeddings(
    nodeId: Long,
    repository: VectorRepository,
    newInputNeighbours: List<NodeEntity>? = null
): Map<String, FloatArray?> {

    val neighbourNodes = mutableListOf<NodeEntity>()

    if (newInputNeighbours != null) {
        neighbourNodes.addAll(newInputNeighbours)
    } else {
        neighbourNodes.addAll(repository.getNeighborsOfNodeById(nodeId))
    }

    val embeddings = mutableMapOf<String, FloatArray?>()

    for (node in neighbourNodes) {
        if (node.type in SchemaPropertyNodes) {
            embeddings[node.type] = node.embedding
        }
    }

    return embeddings
}

fun computeSemanticSimilarity(
    nodeId1: Long,
    nodeId2: Long,
    repository: VectorRepository,
    threshold: Float = 0.5f,
    newInputNeighbours: List<NodeEntity>? = null
): Float {

    val e1 = getPropertyEmbeddings(nodeId1, repository, newInputNeighbours)
    val e2 = getPropertyEmbeddings(nodeId2, repository)

    val similarities = mutableListOf<Float>()
    for (prop in SchemaPropertyNodes) {
        val v1 = e1[prop]
        val v2 = e2[prop]
        if (v1 == null || v2 == null) continue

        val similarity = repository.cosineDistance(v1, v2)
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
    val nodeIds = allNodes
        .filter { it.type in SchemaKeyNodes }
        .map { it.id }

    val simMatrix = mutableMapOf<Pair<Long, Long>, Float>()
    if (nodeIds.isNotEmpty()) {
        for (i in nodeIds) {
            for (j in nodeIds) {
                val score = if (i == j) {
                    1f
                } else {
                    computeSemanticSimilarity(i, j, repository, threshold)
                }
                simMatrix[i to j] = score
            }
        }
    }

    Log.d("INITIALISE MATRIX", "INITIALISED MATRIX")
    return simMatrix
}

fun updateSemanticSimilarityMatrix(
    repository: VectorRepository,
    simMatrix: MutableMap<Pair<Long, Long>, Float>,
    newEventMap: Map<String, String>,
    threshold: Float = 0.5f
): Map<Pair<Long, Long>, Float> {

    val allNodes = repository.getAllNodes()
    val allNodeIds = allNodes
        .filter { it.type in SchemaKeyNodes }
        .map { it.id }

    val newNodeIds = newEventMap.entries
        .filter{ it.key in SchemaKeyNodes }
        .mapNotNull { (type, name) ->
        repository.getNodeByNameAndType(name, type)?.id
    }

    for (newId in newNodeIds) {
        for (otherId in allNodeIds) {
            val score = if (newId == otherId) {
                1f
            } else {
                computeSemanticSimilarity(newId, otherId, repository, threshold)
            }
            // Update both (newId, otherId) and (otherId, newId)
            simMatrix[newId to otherId] = score
            simMatrix[otherId to newId] = score
        }
    }
    Log.d("UPDATE MATRIX", "UPDATED MATRIX")
    return simMatrix
}

suspend fun computeSemanticMatrixForQuery(
    repository: VectorRepository,
    simMatrix: Map<Pair<Long, Long>, Float>,
    newEventMap: Map<String, String>,
    inputEventType: String,
    threshold: Float = 0.5f
): Pair<Map<Pair<Long, Long>, Float>, List<NodeEntity>> {

    val allNodes = repository.getAllNodes()
    val allKeyNodeIds = allNodes
        .filter { it.type == inputEventType }
        .map { it.id }

    // Get filtered similarity matrix
    val filteredSimMatrix = simMatrix.filter { (keyPair, _) ->
        keyPair.first in allKeyNodeIds || keyPair.second in allKeyNodeIds
    }.toMutableMap()

    // 2. Get IDs of the newly added nodes
    val newKeyNode = newEventMap.entries.filter{ it.key in SchemaKeyNodes }
        .map { (type, name) ->
            NodeEntity(id = (-1L * (1..1_000_000).random()), name = name, type = type, embedding = repository.getTextEmbeddings(name))
        }.single()

    val newPropertyNodes = newEventMap.entries.filter{ it.key in SchemaPropertyNodes }
        .map { (type, name) ->
            NodeEntity(id = (-1L * (1..1_000_000).random()),name = name, type = type, embedding = repository.getTextEmbeddings(name))
        }


    for (otherId in allKeyNodeIds) {

        val score = computeSemanticSimilarity(newKeyNode.id, otherId, repository, threshold, newPropertyNodes)

        filteredSimMatrix[newKeyNode.id to otherId] = score
        filteredSimMatrix[otherId to newKeyNode.id] = score
    }

    Log.d("FILTERED MATRIX", "FILTERED MATRIX")
    return filteredSimMatrix to (newPropertyNodes + newKeyNode)
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









