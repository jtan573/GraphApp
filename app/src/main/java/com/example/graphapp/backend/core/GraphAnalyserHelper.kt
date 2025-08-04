package com.example.graphapp.backend.core

import android.util.Log
import com.example.graphapp.backend.dto.GraphSchema
import com.example.graphapp.backend.dto.GraphSchema.PropertyNames
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.backend.dto.GraphSchema.SchemaComputedPropertyNodes
import com.example.graphapp.backend.dto.GraphSchema.SchemaKeyNodes
import com.example.graphapp.backend.dto.GraphSchema.SchemaSemanticPropertyNodes
import com.example.graphapp.backend.schema.EventEmbeddingSet
import com.example.graphapp.backend.schema.EventMetadata
import com.example.graphapp.backend.schema.ExplainedSimilarityWithScores
import com.example.graphapp.backend.schema.SimilarEventTags
import com.example.graphapp.backend.usecases.restoreLocationFromString
import com.example.graphapp.data.repository.PosTaggerRepository
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
): EventEmbeddingSet {

    val neighbourNodes = mutableListOf<EventNodeEntity>()

    if (newInputNeighbours != null) {
        neighbourNodes.addAll(newInputNeighbours)
    } else {
        neighbourNodes.addAll(repository.getNeighborsOfEventNodeById(nodeId))
        neighbourNodes.add(repository.getEventNodeById(nodeId)!!)
    }

    val semanticProps = mutableMapOf<String, EventMetadata>()
    val computedProps = mutableMapOf<String, EventMetadata>()

    for (node in neighbourNodes) {
        if (node.type in (SchemaSemanticPropertyNodes)) {
            semanticProps.put(
                node.type,
                EventMetadata(
                    eventName = node.name,
                    eventEmbeddings = node.embedding,
                    eventTags = node.tags
                )
            )
        }
        if (node.type in SchemaComputedPropertyNodes) {
            computedProps.put(node.type, EventMetadata(eventName = node.name))
        }
    }

    return EventEmbeddingSet(
        semanticProps = semanticProps,
        computedProps = computedProps
    )
}

suspend fun computeSemanticSimilarity(
    nodeId1: Long,
    nodeId2: Long,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    threshold: Float = 0.5f,
    newInputNeighbours: List<EventNodeEntity>? = null,
    thresholdDistance: Float? = 5000f,
    explainSimilarity: Boolean? = false
): Pair<Float, List<SimilarEventTags>> {

    // Get similarity scores (cosine distance)
    val e1 = getPropertyEmbeddings(nodeId1, eventRepository, newInputNeighbours)
    val e2 = getPropertyEmbeddings(nodeId2, eventRepository)
    val similarities = mutableListOf<Float>()

    val simEventsByType = mutableListOf<SimilarEventTags>()

    if (e1.semanticProps != null && e2.semanticProps != null) {
        for (prop in (SchemaSemanticPropertyNodes)) {
            val v1 = e1.semanticProps[prop]
            val v2 = e2.semanticProps[prop]
            if (v1?.eventEmbeddings == null || v2?.eventEmbeddings == null) continue

            val similarity = embeddingRepository.cosineDistance(v1.eventEmbeddings, v2.eventEmbeddings)
            similarities.add(similarity)

            // Consider extracting the tags?
            if (explainSimilarity == true && similarity > 0.4f) {

                val v1TagEmbeddings = v1.eventTags?.map { it to embeddingRepository.getTextEmbeddings(it) }
                val v2TagEmbeddings = v2.eventTags?.map { it to embeddingRepository.getTextEmbeddings(it) }

                if (v1TagEmbeddings != null && v2TagEmbeddings != null) {
                    val tagsFromA = mutableSetOf<String>()
                    val tagsFromB = mutableSetOf<String>()

                    for ((tagA, embeddingA) in v1TagEmbeddings) {
                        for ((tagB, embeddingB) in v2TagEmbeddings) {
                            val sim = embeddingRepository.cosineDistance(embeddingA, embeddingB)
                            if (sim >= 0.5f) {
                                tagsFromA.add(tagA)
                                tagsFromB.add(tagB)
                            }
                        }
                    }
                    simEventsByType.add(SimilarEventTags(
                        propertyType = prop,
                        tagsA = v1.eventTags, tagsB = v2.eventTags,
                        relevantTagsA = tagsFromA.toList(), relevantTagsB = tagsFromB.toList()
                    ))
                }
            }
        }
    }

    if (e1.computedProps != null && e2.computedProps != null) {
        for (prop in SchemaComputedPropertyNodes) {
            val v1 = e1.computedProps[prop]
            val v2 = e2.computedProps[prop]
            if (v1?.eventName == null || v2?.eventName == null) continue

            if (prop == PropertyNames.WHERE.key) {
                val distance = restoreLocationFromString(v1.eventName).distanceTo(restoreLocationFromString(v2.eventName))
                if (distance < 3000f) {
                    similarities.add(1f - (distance / thresholdDistance!!))
                } else {
                    similarities.add(0f)
                }
            }
        }
    }

    if (similarities.isEmpty()) return 0f to emptyList()

    return similarities.average().toFloat() to simEventsByType
}

suspend fun initialiseSemanticSimilarityMatrix(
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
                val result = if (i == j) {
                    1f to emptyList()
                } else {
                    computeSemanticSimilarity(i, j, eventRepository, embeddingRepository, threshold)
                }
                simMatrix[i to j] = result.first
            }
        }
    }

    Log.d("INITIALISE MATRIX", "INITIALISED MATRIX")
    return simMatrix
}

//suspend fun updateSemanticSimilarityMatrix(
//    eventRepository: EventRepository,
//    embeddingRepository: EmbeddingRepository,
//    simMatrix: MutableMap<Pair<Long, Long>, Float>,
//    newEventMap: Map<String, String>,
//    threshold: Float = 0.5f
//): Map<Pair<Long, Long>, Float> {
//
//    val allNodes = eventRepository.getAllEventNodes()
//    val allNodeIds = allNodes
//        .filter { it.type in SchemaKeyNodes }
//        .map { it.id }
//
//    val newNodeIds = newEventMap.entries
//        .filter{ it.key in SchemaKeyNodes }
//        .mapNotNull { (type, name) ->
//            eventRepository.getEventNodeByNameAndType(name, type)?.id
//    }
//
//    for (newId in newNodeIds) {
//        for (otherId in allNodeIds) {
//            val result = if (newId == otherId) {
//                1f to emptyList()
//            } else {
//                computeSemanticSimilarity(newId, otherId, eventRepository, embeddingRepository, threshold)
//            }
//            // Update both (newId, otherId) and (otherId, newId)
//            simMatrix[newId to otherId] = result.first
//            simMatrix[otherId to newId] = result.first
//        }
//    }
//    Log.d("UPDATE MATRIX", "UPDATED MATRIX")
//    return simMatrix
//}

//suspend fun computeSemanticMatrixForQuery(
//    eventRepository: EventRepository,
//    embeddingRepository: EmbeddingRepository,
//    simMatrix: Map<Pair<Long, Long>, Float>,
//    newEventMap: Map<String, String>,
//    inputEventType: String,
//    threshold: Float = 0.5f
//): Pair<Map<Pair<Long, Long>, Float>, List<EventNodeEntity>> {
//
//    val allNodes = eventRepository.getAllEventNodes()
//    val allKeyNodeIds = allNodes.filter { it.type == inputEventType }.map { it.id }
//
//    // Get filtered similarity matrix
//    val filteredSimMatrix = simMatrix.filter { (keyPair, _) ->
//        keyPair.first in allKeyNodeIds || keyPair.second in allKeyNodeIds
//    }.toMutableMap()
//
//    // Get IDs of the newly added nodes
//    val newKeyNode = newEventMap.entries.filter{ it.key in SchemaKeyNodes }
//        .map { (type, name) ->
//            EventNodeEntity(
//                id = (-1L * (1..1_000_000).random()),
//                name = name,
//                type = type,
//                embedding = embeddingRepository.getTextEmbeddings(name)
//            )
//        }.single()
//
//    val newPropertyNodes = newEventMap.entries.filter{ it.key in SchemaPropertyNodes }
//        .map { (type, name) ->
//            EventNodeEntity(
//                id = (-1L * (1..1_000_000).random()),
//                name = name,
//                type = type,
//                embedding = embeddingRepository.getTextEmbeddings(name)
//            )
//        }
//
//
//    for (otherId in allKeyNodeIds) {
//
//        val score = computeSemanticSimilarity(newKeyNode.id, otherId, eventRepository, embeddingRepository, threshold, newPropertyNodes)
//
//        filteredSimMatrix[newKeyNode.id to otherId] = score
//        filteredSimMatrix[otherId to newKeyNode.id] = score
//    }
//
//    Log.d("FILTERED MATRIX", "FILTERED MATRIX")
//    return filteredSimMatrix to (newPropertyNodes + newKeyNode)
//}

suspend fun computeSemanticSimilarEventsForProps(
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    newEventMap: Map<String, EventNodeEntity>,
    queryKey: String? = null,
    getTopThreeResultsOnly: Boolean = true,
    threshold: Float = 0.0f,
): Map<String, List<ExplainedSimilarityWithScores>> {

    val allNodes = eventRepository.getAllEventNodes()

    var allKeyNodeIdsByType = mapOf<String, List<Long>>()
    allKeyNodeIdsByType = if (queryKey != null) {
        allNodes.filter { it.type == queryKey }.groupBy { it.type }.mapValues { (_, nodes) -> nodes.map { it.id } }
    } else {
        allNodes.filter { it.type in SchemaKeyNodes }.groupBy { it.type }.mapValues { (_, nodes) -> nodes.map { it.id } }
    }

    val propsInEvent = newEventMap.values.toList()

    val simMatrix = mutableMapOf<String, MutableList<ExplainedSimilarityWithScores>>()

    for ((eventType, keyNodeIds) in allKeyNodeIdsByType) {
        for (keyNodeId in keyNodeIds) {
            val sim = computeSemanticSimilarity(
                nodeId1 = -0L,
                nodeId2 = keyNodeId,
                eventRepository = eventRepository,
                embeddingRepository = embeddingRepository,
                threshold = threshold,
                newInputNeighbours = propsInEvent,
                explainSimilarity = true
            )
            val freqFactor = ln(1f + (eventRepository.getEventNodeFrequencyOfNodeId(keyNodeId) ?: 1)).toFloat()

            val adjustedSim = sim.first * freqFactor
            if (adjustedSim > threshold) {
                simMatrix.getOrPut(eventType) { mutableListOf() }.add(
                    ExplainedSimilarityWithScores(
                        simScore = adjustedSim,
                        targetNodeId = keyNodeId,
                        explainedSimilarity = sim.second
                ))
            }
        }
    }

    return if (getTopThreeResultsOnly) {
        simMatrix.mapValues { (_, similarityWithScores) ->
            similarityWithScores.sortedByDescending { it.simScore }.take(3)
        }
    } else {
        simMatrix.mapValues { (_, pairs) ->
            pairs.sortedByDescending { it.simScore }
        }
    }
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
//fun computeTopRecommendations(
//    inputKeyNode: EventNodeEntity,
//    repository: EventRepository,
//    simMatrix: Map<Pair<Long, Long>, Float>,
//    queryKey: String? = null
//): MutableMap<String, MutableList<EventNodeEntity>> {
//
//    val result = mutableMapOf<String, MutableList<EventNodeEntity>>()
//
//    // 1. Find all nodes of same event type
//    val allNodesOfEventType = repository.getAllEventNodes().filter { it.type == inputKeyNode.type }
//
//    val scores = mutableListOf<Pair<EventNodeEntity, Float>>()
//
//    // 2. Compute similarities
//    for (candidate in allNodesOfEventType) {
//        if (candidate.id == inputKeyNode.id) continue
//
//        val simScore = computeWeightedSim(
//            targetId = candidate.id,
//            candidateId = inputKeyNode.id,
//            repository = repository,
//            similarityMatrix = simMatrix
//        ).toFloat()
//
//        scores.add(candidate to simScore)
//
//        Log.d("TOPFORTYPE", "${candidate.name}: $simScore")
//    }
//
//    // 3. Sort & take top 3
//    val topForType = scores.sortedByDescending { it.second }.take(3)
//    topForType.forEach { (top, score) ->
//        Log.d("TOPFORTYPE", "${top.name}: $score")
//    }
//
//    // 4. Populate recommendations
//    for ((simNode, _) in topForType) {
//
//        if (queryKey == null || queryKey == simNode.type) {
//            result.getOrPut(simNode.type) { mutableListOf() }.add(simNode)
//        }
//
//        val neighborKeyNodes = repository.getNeighborsOfEventNodeById(simNode.id)
//            .filter { it.type in SchemaKeyNodes }
//
//        for (neighbor in neighborKeyNodes) {
//            if (queryKey == null || queryKey == neighbor.type) {
//                result.getOrPut(neighbor.type) { mutableListOf() }.add(neighbor)
//            }
//        }
//    }
//
//    return result
//}




