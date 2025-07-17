package com.example.graphapp.data.local

import android.util.Log
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.schema.GraphSchema.SchemaKeyNodes
import com.example.graphapp.data.schema.GraphSchema.SchemaPropertyNodes
import kotlin.collections.iterator
import kotlin.math.ln

/* -------------------------------------------------
  Helper to compute Weighted Similarity between nodes
------------------------------------------------- */
fun computeWeightedSim(
    targetId: Long,
    candidateId: Long,
    repository: EventRepository,
    similarityMatrix: Map<Pair<Long, Long>, Float>
): Float {

    val occurrenceCounts = repository.getAllEventNodeFrequencies()
    val rawSim = similarityMatrix.getOrDefault(targetId to candidateId, 0f)
    val freqFactor = ln(1f + (occurrenceCounts[candidateId] ?: 1)).toFloat()
    val adjustedSim = rawSim * freqFactor

    return adjustedSim
}

/* -------------------------------------------------
  Helpers to initialise Semantic Similarity Matrix
------------------------------------------------- */
fun getPropertyEmbeddings(
    nodeId: Long,
    repository: EventRepository,
    newInputNeighbours: List<EventNodeEntity>? = null
): Map<String, FloatArray?> {

    val neighbourNodes = mutableListOf<EventNodeEntity>()

    if (newInputNeighbours != null) {
        neighbourNodes.addAll(newInputNeighbours)
    } else {
        neighbourNodes.addAll(repository.getNeighborsOfEventNodeById(nodeId))
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
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    threshold: Float = 0.5f,
    newInputNeighbours: List<EventNodeEntity>? = null
): Float {

    val e1 = getPropertyEmbeddings(nodeId1, eventRepository, newInputNeighbours)
    val e2 = getPropertyEmbeddings(nodeId2, eventRepository)

    val similarities = mutableListOf<Float>()
    for (prop in SchemaPropertyNodes) {
        val v1 = e1[prop]
        val v2 = e2[prop]
        if (v1 == null || v2 == null) continue

        val similarity = embeddingRepository.cosineDistance(v1, v2)
        similarities.add(similarity)
    }

    if (similarities.isEmpty()) return 0f

    return similarities.average().toFloat()
}

fun initialiseSemanticSimilarityMatrix(
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,

    threshold: Float = 0.5f
): Map<Pair<Long, Long>, Float> {

    val allNodes = eventRepository.getAllEventNodes()
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
                    computeSemanticSimilarity(i, j, eventRepository, embeddingRepository, threshold)
                }
                simMatrix[i to j] = score
            }
        }
    }

    Log.d("INITIALISE MATRIX", "INITIALISED MATRIX")
    return simMatrix
}

fun updateSemanticSimilarityMatrix(
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    simMatrix: MutableMap<Pair<Long, Long>, Float>,
    newEventMap: Map<String, String>,
    threshold: Float = 0.5f
): Map<Pair<Long, Long>, Float> {

    val allNodes = eventRepository.getAllEventNodes()
    val allNodeIds = allNodes
        .filter { it.type in SchemaKeyNodes }
        .map { it.id }

    val newNodeIds = newEventMap.entries
        .filter{ it.key in SchemaKeyNodes }
        .mapNotNull { (type, name) ->
            eventRepository.getEventNodeByNameAndType(name, type)?.id
    }

    for (newId in newNodeIds) {
        for (otherId in allNodeIds) {
            val score = if (newId == otherId) {
                1f
            } else {
                computeSemanticSimilarity(newId, otherId, eventRepository, embeddingRepository, threshold)
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
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    simMatrix: Map<Pair<Long, Long>, Float>,
    newEventMap: Map<String, String>,
    inputEventType: String,
    threshold: Float = 0.5f
): Pair<Map<Pair<Long, Long>, Float>, List<EventNodeEntity>> {

    val allNodes = eventRepository.getAllEventNodes()
    val allKeyNodeIds = allNodes.filter { it.type == inputEventType }.map { it.id }

    // Get filtered similarity matrix
    val filteredSimMatrix = simMatrix.filter { (keyPair, _) ->
        keyPair.first in allKeyNodeIds || keyPair.second in allKeyNodeIds
    }.toMutableMap()

    // Get IDs of the newly added nodes
    val newKeyNode = newEventMap.entries.filter{ it.key in SchemaKeyNodes }
        .map { (type, name) ->
            EventNodeEntity(
                id = (-1L * (1..1_000_000).random()),
                name = name,
                type = type,
                embedding = embeddingRepository.getTextEmbeddings(name)
            )
        }.single()

    val newPropertyNodes = newEventMap.entries.filter{ it.key in SchemaPropertyNodes }
        .map { (type, name) ->
            EventNodeEntity(
                id = (-1L * (1..1_000_000).random()),
                name = name,
                type = type,
                embedding = embeddingRepository.getTextEmbeddings(name)
            )
        }


    for (otherId in allKeyNodeIds) {

        val score = computeSemanticSimilarity(newKeyNode.id, otherId, eventRepository, embeddingRepository, threshold, newPropertyNodes)

        filteredSimMatrix[newKeyNode.id to otherId] = score
        filteredSimMatrix[otherId to newKeyNode.id] = score
    }

    Log.d("FILTERED MATRIX", "FILTERED MATRIX")
    return filteredSimMatrix to (newPropertyNodes + newKeyNode)
}

fun computeSemanticSimilarEventsForProps(
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    onlyPropertiesMap: Map<String, EventNodeEntity>,
    queryKey: String? = null,
    threshold: Float = 0.5f
): Map<String, List<Pair<Long, Float>>>{

    val allNodes = eventRepository.getAllEventNodes()

    var allKeyNodeIdsByType = mapOf<String, List<Long>>()
    if (queryKey != null) {
        allKeyNodeIdsByType = allNodes.filter { it.type == queryKey }.groupBy { it.type }
            .mapValues { (_, nodes) -> nodes.map { it.id } }
    } else {
        allKeyNodeIdsByType = allNodes.filter { it.type in SchemaKeyNodes }.groupBy { it.type }
            .mapValues { (_, nodes) -> nodes.map { it.id } }
    }

    val propsInEvent = onlyPropertiesMap.values.toList()

    val simMatrix = mutableMapOf<String, MutableList<Pair<Long, Float>>>()

    for ((eventType, keyNodeIds) in allKeyNodeIdsByType) {
        for (keyNodeId in keyNodeIds) {
            val sim = computeSemanticSimilarity(-0L, keyNodeId, eventRepository, embeddingRepository, threshold, propsInEvent)
            val freqFactor = ln(1f + (eventRepository.getEventNodeFrequencyOfNodeId(keyNodeId) ?: 1)).toFloat()

            val adjustedSim = sim * freqFactor
            if (adjustedSim > threshold) {
                simMatrix.getOrPut(eventType) { mutableListOf() }.add(keyNodeId to adjustedSim)
            }
        }
    }

    val topEventsByType = simMatrix.mapValues { (_, pairs) ->
        pairs.sortedByDescending { it.second }.take(3)
    }

    return topEventsByType
}


/* -------------------------------------------------
    Helper to build graph
------------------------------------------------- */
fun buildGraphContext(
    repository: EventRepository,
    predictedNodeIds: Collection<Long>,
    extraNodes: Collection<EventNodeEntity> = emptyList(),
    addSuggestionEdges: (
        MutableSet<EventEdgeEntity>,
        Set<EventNodeEntity>
    ) -> Unit
): Pair<MutableSet<EventNodeEntity>, MutableSet<EventEdgeEntity>> {

    val neighborNodes = mutableSetOf<EventNodeEntity>()
    val neighborEdges = mutableSetOf<EventEdgeEntity>()

    for (id in predictedNodeIds) {
        val nodes = repository.getNeighborsOfEventNodeById(id)
        for (n in nodes) {
            if (id == n.id) continue
            val edge = repository.getEdgeBetweenEventNodes(id, n.id)
            neighborEdges.add(edge)
        }

        nodes.forEach { node ->
            if (neighborNodes.none { it.id == node.id }) { neighborNodes.add(node) }
        }
        val node = repository.getEventNodeById(id)
        if (node != null && neighborNodes.none { it.id == node.id }) {
            neighborNodes.add(node)
        }
    }

    // Add any explicitly supplied extra nodes
    neighborNodes += extraNodes

    // Let the caller add any custom edges
    addSuggestionEdges(neighborEdges, neighborNodes)

    return neighborNodes.toMutableSet() to neighborEdges.toMutableSet()
}

/* -------------------------------------------------
    Helper to load data from cache
------------------------------------------------- */
fun loadCachedRecommendations(
    inputKeyNode: EventNodeEntity,
    repository: EventRepository,
    queryKey: String?
): MutableMap<String, MutableList<EventNodeEntity>> {
    val result = mutableMapOf<String, MutableList<EventNodeEntity>>()

    if (inputKeyNode.cachedNodeIds.isNotEmpty()) {
        if (queryKey != null) {
            val cachedQueryIds = inputKeyNode.cachedNodeIds[queryKey]
            if (cachedQueryIds != null) {
                for (id in cachedQueryIds) {
                    result.getOrPut(queryKey) { mutableListOf() }
                        .add(repository.getEventNodeById(id)!!)
                }
            }
        } else {
            for ((type, cacheIds) in inputKeyNode.cachedNodeIds) {
                for (id in cacheIds) {
                    result.getOrPut(type) { mutableListOf() }
                        .add(repository.getEventNodeById(id)!!)
                }
            }
        }
    }

    return result
}

/* -------------------------------------------------
    Helper to calculate top recommendations
------------------------------------------------- */
fun computeTopRecommendations(
    inputKeyNode: EventNodeEntity,
    repository: EventRepository,
    simMatrix: Map<Pair<Long, Long>, Float>,
    queryKey: String? = null
): MutableMap<String, MutableList<EventNodeEntity>> {

    val result = mutableMapOf<String, MutableList<EventNodeEntity>>()

    // 1. Find all nodes of same event type
    val allNodesOfEventType = repository.getAllEventNodes().filter { it.type == inputKeyNode.type }

    val scores = mutableListOf<Pair<EventNodeEntity, Float>>()

    // 2. Compute similarities
    for (candidate in allNodesOfEventType) {
        if (candidate.id == inputKeyNode.id) continue

        val simScore = computeWeightedSim(
            targetId = candidate.id,
            candidateId = inputKeyNode.id,
            repository = repository,
            similarityMatrix = simMatrix
        ).toFloat()

        scores.add(candidate to simScore)
    }

    // 3. Sort & take top 3
    val topForType = scores.sortedByDescending { it.second }.take(3)

    // 4. Populate recommendations
    for ((simNode, _) in topForType) {

        if (queryKey == null || queryKey == simNode.type) {
            result.getOrPut(simNode.type) { mutableListOf() }.add(simNode)
        }

        val neighborKeyNodes = repository.getNeighborsOfEventNodeById(simNode.id)
            .filter { it.type in SchemaKeyNodes }

        for (neighbor in neighborKeyNodes) {
            if (queryKey == null || queryKey == neighbor.type) {
                result.getOrPut(neighbor.type) { mutableListOf() }.add(neighbor)
            }
        }
    }

    return result
}




