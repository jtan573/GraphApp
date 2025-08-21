package com.example.graphapp.backend.core

import android.util.Log
import com.example.graphapp.backend.core.GraphSchema
import com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.backend.core.GraphSchema.SchemaComputedPropertyNodes
import com.example.graphapp.backend.core.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.backend.core.GraphSchema.SchemaKeyNodes
import com.example.graphapp.backend.core.GraphSchema.SchemaSemanticPropertyNodes
import com.example.graphapp.backend.usecases.restoreLocationFromString
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
            if (explainSimilarity == true) {

                val v1TagEmbeddings = v1.eventTags?.map { it to embeddingRepository.getTextEmbeddings(it) }
                val v2TagEmbeddings = v2.eventTags?.map { it to embeddingRepository.getTextEmbeddings(it) }

                if (v1TagEmbeddings != null && v2TagEmbeddings != null) {
                    val tagsFromA = mutableSetOf<String>()
                    val tagsFromB = mutableSetOf<String>()

                    for ((tagA, embeddingA) in v1TagEmbeddings) {
                        for ((tagB, embeddingB) in v2TagEmbeddings) {
                            val sim = embeddingRepository.cosineDistance(embeddingA, embeddingB)
                            if (sim >= 0.4f) {
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

            if (prop == SchemaEventTypeNames.WHERE.key) {
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

suspend fun computeSemanticSimilarEventsForProps(
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    newEventMap: Map<String, EventNodeEntity>,
    sourceEventType: SchemaKeyEventTypeNames? = null,
    targetEventType: SchemaKeyEventTypeNames? = null,
    getTopThreeResultsOnly: Boolean = true,
    threshold: Float = 0.0f,
    activeNodesOnly: Boolean
): Map<String, List<ExplainedSimilarityWithScores>> {

    // Get node status
    val nodeStatus = if (activeNodesOnly) {
        listOf(EventStatus.ACTIVE)
    } else {
        listOf(EventStatus.INACTIVE, EventStatus.ACTIVE)
    }

    // Only obtain nodes with relevant tags
    var allKeyNodeIdsByType = mutableMapOf<String, MutableList<Long>>()
    newEventMap.forEach { (type, eventNode) ->
        var nodes: List<EventNodeEntity> = if (type in SchemaSemanticPropertyNodes) {
            // Semantic nodes
            eventRepository.getRelevantNodes(eventNode.tags, type).filter { it.status in nodeStatus }
        } else {
            // Computed Nodes
            eventRepository.getCloseNodesByLocation(eventNode.name).filter { it.status in nodeStatus }
        }

        Log.d("CHECKPOINT 1:", "nodes with similar tags: ${nodes.joinToString { it.name }}")

        // Retrieve key neighbours of relevant nodes
        nodes.forEach { node ->
            var keyNeighbours: List<EventNodeEntity> =
                if (sourceEventType != null) {
                    eventRepository.getNeighborsOfEventNodeById(node.id)
                        .filter { it.type.lowercase() == sourceEventType.toString().lowercase() }
                } else {
                    eventRepository.getNeighborsOfEventNodeById(node.id)
                        .filter { it.type in SchemaKeyNodes }
                }
            keyNeighbours.forEach {
                val list = allKeyNodeIdsByType.getOrPut(it.type) { mutableListOf() }
                if (it.id !in list) {
                    list.add(it.id)
                }
            }
            if (node.type in SchemaKeyNodes) {
                val nodeList = allKeyNodeIdsByType.getOrPut(node.type) { mutableListOf() }
                if (node.id !in nodeList) {
                    nodeList.add(node.id)
                }
            }
        }

        Log.d("CHECKPOINT 2:", "neighbourNodes: $allKeyNodeIdsByType")
    }

    // Get values in new event
    val propsInEvent = newEventMap.values.toList()

    // Create temporary similarity matrix
    val simMatrix = mutableMapOf<String, MutableList<ExplainedSimilarityWithScores>>()

    // Compute semantic similarity between key node pairs
    for ((_, keyNodeIds) in allKeyNodeIdsByType) {
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

            Log.d("CHECKPOINT 3:", "keyNodeID: $keyNodeId -> sim score: $sim")

            val mainNodes = eventRepository.getNeighborsOfEventNodeById(keyNodeId).toMutableList()
            mainNodes.add(eventRepository.getEventNodeById(keyNodeId)!!)
            val simNodesIds = if (targetEventType != null) {
                mainNodes.filter{ it.type.lowercase() == targetEventType.toString().lowercase() }.map { it.type to it.id}
            } else {
                mainNodes.filter{ node -> SchemaKeyEventTypeNames.fromKey(node.type) != null }
                    .map { it.type to it.id}
            }

            simNodesIds.forEach { (type, nodeId) ->
                val freqFactor = ln(1f + (eventRepository.getEventNodeFrequencyOfNodeId(nodeId) ?: 1)).toFloat()
                val adjustedSim = sim.first * freqFactor

                if (adjustedSim > threshold) {
                    simMatrix.getOrPut(type) { mutableListOf() }.add(
                        ExplainedSimilarityWithScores(
                            simScore = adjustedSim,
                            targetNodeId = nodeId,
                            explainedSimilarity = sim.second
                        ))
                }
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




