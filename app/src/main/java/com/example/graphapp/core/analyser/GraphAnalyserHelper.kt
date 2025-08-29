package com.example.graphapp.core.analyser

import com.example.graphapp.core.schema.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.core.schema.GraphSchema.SchemaComputedPropertyNodes
import com.example.graphapp.core.schema.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.core.schema.GraphSchema.SchemaKeyNodes
import com.example.graphapp.core.schema.GraphSchema.SchemaSemanticPropertyNodes
import com.example.graphapp.core.config.SimilarityConfig
import com.example.graphapp.core.usecases.restoreLocationFromString
import java.util.concurrent.TimeUnit
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.math.abs
import kotlin.math.ln

/**
 * Prepares the data required for computation and returns similarity results.
 *
 * @param eventRepository Repository of all events in the db.
 * @param embeddingRepository Repository of embedding model.
 * @param allKeyNodeIdsByType Map of property type to list of relevant node's IDs
 * @param targetEventType Event type of target event.
 * @param numTopResults Number of top results.
 * @param propsInEvent List of nodes of properties in input event
 * @return Map of event type to the details of the similar event found.
 */
suspend fun generateSimilarityResults(
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    allKeyNodeIdsByType: Map<String, MutableList<Long>>,
    targetEventType: SchemaKeyEventTypeNames? = null,
    numTopResults: Int = SimilarityConfig.NUM_TOP_RESULTS_REQUIRED,
    propsInEvent: List<EventNodeEntity>
): Map<String, List<ExplainedSimilarityWithScores>> {

    // Create temporary similarity matrix
    val simMatrix = mutableMapOf<String, MutableList<ExplainedSimilarityWithScores>>()

    // Compute semantic similarity between key node pairs
    for ((_, keyNodeIds) in allKeyNodeIdsByType) {
        for (keyNodeId in keyNodeIds) {
            val sim = computeOverallSimilarity(
                nodeId1 = -0L,
                nodeId2 = keyNodeId,
                eventRepository = eventRepository,
                embeddingRepository = embeddingRepository,
                newInputNeighbours = propsInEvent,
                explainSimilarity = true
            )

            // Get all nodes of the entity, 5W1H
            val mainNodes = eventRepository.getNeighborsOfEventNodeById(keyNodeId).toMutableList()
            mainNodes.add(eventRepository.getEventNodeById(keyNodeId)!!)

            // If target event type is given, then extract only nodes of that type.
            // Else, take all nodes.
            val simNodesIds = if (targetEventType != null) {
                mainNodes.filter{ it.type.lowercase() == targetEventType.toString().lowercase() }.map { it.type to it.id }
            } else {
                mainNodes.filter{ node -> SchemaKeyEventTypeNames.fromKey(node.type) != null }
                    .map { it.type to it.id }
            }

            // Weigh similarity using frequency
            simNodesIds.forEach { (type, nodeId) ->
                val adjustedSim = getWeightedSimilarityScore(nodeId, eventRepository, sim.first)
                simMatrix.getOrPut(type) { mutableListOf() }.add(
                    ExplainedSimilarityWithScores(
                        simScore = adjustedSim,
                        targetNodeId = nodeId,
                        explainedSimilarity = sim.second
                    ))
            }
        }
    }

    return simMatrix.mapValues { (_, similarityWithScores) ->
        similarityWithScores.sortedByDescending { it.simScore }.take(numTopResults)
    }
}

/**
 * Retrieves embeddings of the 5W1H of a specified node from the database.
 * 
 * @param nodeId Id of the target node.
 * @param repository Repository of all events in the database.
 * @param newInputNeighbours A list of nodes representing 5W1H when the node is not present in the database. 
 * @return Set of embeddings, split into Semantic and Computed.
 */
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

/**
 * Computes overall similarity between two nodes.
 * 
 * @param nodeId1 Id of the first node to compare.
 * @param nodeId2 Id of the second node to compare.
 * @param eventRepository Repository of all events in the db.
 * @param embeddingRepository Repository of embedding model.
 * @param newInputNeighbours List of nodes (5W1H) when nodes are not present in the db.
 * @param explainSimilarity Get similar tags between properties.
 * @return Pair of similarity score to a list of objects describing the similar properties.
 */
suspend fun computeOverallSimilarity(
    nodeId1: Long,
    nodeId2: Long,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    newInputNeighbours: List<EventNodeEntity>? = null,
    explainSimilarity: Boolean = false
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

            val semanticResults = computeSimilarityForSemanticProperties(
                targetProperty = prop,
                eventA = v1,
                eventB = v2,
                embeddingRepository = embeddingRepository,
                explainSimilarity = explainSimilarity
            )
            if (semanticResults != null) {
                simEventsByType.add(semanticResults)
            }
        }
    }

    if (e1.computedProps != null && e2.computedProps != null) {
        for (prop in SchemaComputedPropertyNodes) {
            val v1 = e1.computedProps[prop]
            val v2 = e2.computedProps[prop]
            if (v1?.eventName == null || v2?.eventName == null) continue

            if (prop == SchemaEventTypeNames.WHERE.key) {
                val (simScore, simEventTags) = computeSimilarityForWhereProperty(
                    eventNameA = v1.eventName, eventNameB = v2.eventName
                )
                similarities.add(simScore)
                if (simEventTags != null) {
                    simEventsByType.add(simEventTags)
                }
            }

            if (prop == SchemaEventTypeNames.WHEN.key) {
                val (simScore, simEventTags) = computeSimilarityForWhenProperty(
                    eventNameA = v1.eventName, eventNameB = v2.eventName
                )
                similarities.add(simScore)
                if (simEventTags != null) {
                    simEventsByType.add(simEventTags)
                }
            }
        }
    }

    if (similarities.isEmpty()) return 0f to emptyList()
    return similarities.average().toFloat() to simEventsByType
}

/**
 * Calculate similarity between two semantic nodes.
 *
 * @param targetProperty Property type of both nodes.
 * @param eventA Value of property.
 * @param eventB Value of property.
 * @param embeddingRepository Repository of embedding model.
 * @param explainSimilarity Whether to check similar tags between the two nodes.
 *
 */
suspend fun computeSimilarityForSemanticProperties(
    targetProperty: String,
    eventA: EventMetadata,
    eventB: EventMetadata,
    embeddingRepository: EmbeddingRepository,
    explainSimilarity: Boolean
): SimilarEventTags? {
    if (eventA.eventEmbeddings == null || eventB.eventEmbeddings == null) return null

    val similarities = mutableListOf<Float>()

    val similarity = embeddingRepository.computeCosineSimilarity(eventA.eventEmbeddings, eventB.eventEmbeddings)
    similarities.add(similarity)

    if (explainSimilarity == true) {
        val v1TagEmbeddings = eventA.eventTags?.map { it to embeddingRepository.getTextEmbeddings(it) }
        val v2TagEmbeddings = eventB.eventTags?.map { it to embeddingRepository.getTextEmbeddings(it) }

        if (v1TagEmbeddings != null && v2TagEmbeddings != null) {
            val tagsFromA = mutableSetOf<Pair<String, Float>>()
            val tagsFromB = mutableSetOf<Pair<String, Float>>()

            for ((tagA, embeddingA) in v1TagEmbeddings) {
                for ((tagB, embeddingB) in v2TagEmbeddings) {
                    val sim = embeddingRepository.computeCosineSimilarity(embeddingA, embeddingB)
                    if (sim >= SimilarityConfig.SIMILARITY_THRESHOLD_SEMANTIC_PROPS) {
                        tagsFromA.add(tagA to sim)
                        tagsFromB.add(tagB to sim)
                    }
                }
            }
            return SimilarEventTags(
                propertyType = targetProperty,
                tagsA = eventA.eventTags, tagsB = eventB.eventTags,
                relevantTagsA = tagsFromA.toList(), relevantTagsB = tagsFromB.toList(),
                simScore = similarity
            )
        }
    }
    return null
}

/**
 * Computes similarity between two WHERE nodes.
 * @param eventNameA Value of first node.
 * @param eventNameB Value of second node.
 * @return Pair of similarity score to a tag representing the abs distance value.
 */
fun computeSimilarityForWhereProperty(
    eventNameA: String,
    eventNameB: String
): Pair<Float , SimilarEventTags?> {
    val distance = restoreLocationFromString(eventNameA).distanceTo(restoreLocationFromString(eventNameB))
        if (distance < SimilarityConfig.MAX_DISTANCE) {
        val distanceSim = 1f - (distance / SimilarityConfig.MAX_DISTANCE)

        return distanceSim to SimilarEventTags(
            propertyType = SchemaEventTypeNames.WHERE.key,
            tagsA = listOf(eventNameA),
            tagsB = listOf(eventNameB),
            relevantTagsA = listOf(((SimilarityConfig.SIMILARITY_PRECISION).format(distance))+"m" to distanceSim),
            relevantTagsB = listOf(((SimilarityConfig.SIMILARITY_PRECISION).format(distance))+"m" to distanceSim),
            simScore = distanceSim
        )
    } else {
        return 0f to null
    }
}

/**
 * Computes similarity between two WHEN nodes.
 * @param eventNameA Value of first node.
 * @param eventNameB Value of second node.
 * @return Pair of similarity score to a tag representing the abs time difference value.
 */
fun computeSimilarityForWhenProperty(
    eventNameA: String,
    eventNameB: String
): Pair<Float , SimilarEventTags?> {
    val timeDiff = abs(eventNameA.toLong() - eventNameB.toLong())
    if (timeDiff <= SimilarityConfig.MAX_TIME_DIFFERENCE) {
        val timeSim = 1f - (timeDiff / SimilarityConfig.MAX_TIME_DIFFERENCE).toFloat()
        return timeSim to SimilarEventTags(
            propertyType = SchemaEventTypeNames.WHEN.key,
            tagsA = listOf(eventNameA), tagsB = listOf(eventNameB),
            relevantTagsA = listOf(TimeUnit.MILLISECONDS.toHours(timeDiff).toString()+"h" to timeSim),
            relevantTagsB = listOf(TimeUnit.MILLISECONDS.toHours(timeDiff).toString()+"h" to timeSim),
            simScore = timeSim
        )
    } else {
        return 0f to null
    }
}

/**
 * Converts event input in the form of text to node form.
 *
 * @param newEventMap
 * @param eventRepository
 * @param embeddingRepository
 * @return Map of node type name to node created.
 */
suspend fun convertStringInputToNodes(
    newEventMap: Map<SchemaEventTypeNames, String>,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
): Map<String, EventNodeEntity> {
    var eventNodesByType = mutableMapOf<String, EventNodeEntity>()
    newEventMap.forEach { (type, value) ->
        val nodeExist = eventRepository.getEventNodeByNameAndType(value, type.key)
        if (nodeExist != null) {
            eventNodesByType[type.key] = nodeExist
        } else {
            eventNodesByType[type.key] = eventRepository.getTemporaryEventNode(value, type.key, embeddingRepository)
        }
    }
    return eventNodesByType
}

/**
 * Checks and returns node status(es) to be considered.
 * @param activeNodesOnly Whether we want active nodes only or all nodes.
 * @return List of node status required.
 */
fun checkTargetNodeStatus(
    activeNodesOnly: Boolean
): List<EventStatus> {
    return if (activeNodesOnly) {
        listOf(EventStatus.ACTIVE)
    } else {
        listOf(EventStatus.INACTIVE, EventStatus.ACTIVE)
    }
}

/**
 * Retrieve subset of database for similarity computation. Does the following:
 *      1. Considers required node status (ACTIVE, INACTIVE)
 *      2. Match and retrieves nodes with same tags.
 *      3. Retrieve key event nodes from property nodes.
 *
 * @param newEventMap Map of PropertyName to node representing the property.
 * @param eventRepository Repository of all events in the db.
 * @param nodeStatus List of target node status.
 * @param sourceEventType Event type of input event.
 * @return Map of key event type to list of nodes of same type that are relevant.
 */
suspend fun prepareDatasetForSimilarityComputation(
    newEventMap: Map<String, EventNodeEntity>,
    eventRepository: EventRepository,
    nodeStatus: List<EventStatus>,
    sourceEventType: SchemaKeyEventTypeNames?,
): Map<String, MutableList<Long>> {

    var allKeyNodeIdsByType = mutableMapOf<String, MutableList<Long>>()
    newEventMap.forEach { (type, eventNode) ->
        var nodes: List<EventNodeEntity> = if (type in SchemaSemanticPropertyNodes) {
            // Semantic nodes
            eventRepository.getRelevantNodes(eventNode.tags, type).filter { it.status in nodeStatus }
        } else if (type == SchemaEventTypeNames.WHERE.key) {
            // Computed Nodes
            eventRepository.getCloseNodesByLocation(eventNode.name).filter { it.status in nodeStatus }
        } else {
            eventRepository.getCloseNodesByDatetime(eventNode.name).filter { it.status in nodeStatus }
        }

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
    }
    return allKeyNodeIdsByType
}

/**
 * Computes weighted similarity by checking frequency score of each node.
 * @param nodeId Id of node to be checked
 * @param eventRepository Repository of event database.
 * @param simScore Computed similarity score before freq.
 * @return Weighted similarity score.
 */
fun getWeightedSimilarityScore(
    nodeId: Long,
    eventRepository: EventRepository,
    simScore: Float
): Float {
    val freqFactor = ln(1f + (eventRepository.getEventNodeFrequencyOfNodeId(nodeId) ?: 1)).toFloat()
    return simScore * freqFactor
}




