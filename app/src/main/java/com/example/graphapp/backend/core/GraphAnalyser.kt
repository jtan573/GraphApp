package com.example.graphapp.backend.core

import android.util.Log
import com.example.graphapp.backend.core.GraphSchema
import com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.core.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.api.DiscoverEventsResponse
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.api.KeyNode
import com.example.graphapp.data.api.PatternFindingResponse
import com.example.graphapp.data.api.ReplicaDetectionResponse
import com.example.graphapp.data.api.SimilarEvent
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.backend.core.GraphSchema.SchemaKeyNodes
import com.example.graphapp.backend.core.GraphSchema.SchemaOtherNodes
import com.example.graphapp.backend.core.GraphSchema.SchemaPropertyNodes
//import com.example.graphdb.Edge
//import com.example.graphdb.Node
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

/* -------------------------------------------------
    FUNCTION 1: Find similar events
------------------------------------------------- */
suspend fun computeSimilarAndRelatedEvents (
    newEventMap: Map<SchemaEventTypeNames, String>,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    sourceEventType: SchemaKeyEventTypeNames? = null,
    targetEventType: SchemaKeyEventTypeNames? = null,
    getTopThreeResultsOnly: Boolean = true,
    customThreshold: Float = 0.0f,
    activeNodesOnly: Boolean
) : Triple<List<EventNodeEntity>?, List<EventEdgeEntity>?, DiscoverEventsResponse> {

    var eventNodesByType = mutableMapOf<String, EventNodeEntity>()

    // Get nodes from DB / Create temporary nodes for the search
    newEventMap.entries.map { (type, value) ->
        val nodeExist = eventRepository.getEventNodeByNameAndType(value, type.key)
        if (nodeExist != null) {
            eventNodesByType[type.key] = nodeExist
        } else {
            eventNodesByType[type.key] = eventRepository.getTemporaryEventNode(value, type.key, embeddingRepository)
        }
    }

    // For each key node type, compute top 3
    val topRecommendationsByType = computeSemanticSimilarEventsForProps(
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        newEventMap = eventNodesByType,
        sourceEventType = sourceEventType,
        targetEventType = targetEventType,
        getTopThreeResultsOnly = getTopThreeResultsOnly,
        threshold = customThreshold,
        activeNodesOnly = activeNodesOnly
    )
    Log.d("topRecommendationsByType","$topRecommendationsByType")

    val eventsByType = mutableMapOf<SchemaKeyEventTypeNames, List<EventDetails>>()

    for ((type, recs) in topRecommendationsByType) {
        val predictedEventsList = mutableListOf<EventDetails>()
        for (rec in recs) {
            val neighbourProps = eventRepository.getNeighborsOfEventNodeById(rec.targetNodeId)
                .filter { it.type in (SchemaPropertyNodes + SchemaOtherNodes) }
                .associate { it.type to it.name }

            val recNode = eventRepository.getEventNodeById(rec.targetNodeId)!!
            predictedEventsList.add(
                EventDetails(
                    eventId = recNode.id,
                    eventName = recNode.name,
                    eventProperties = neighbourProps,
                    simScore = rec.simScore,
                    simProperties = rec.explainedSimilarity
                )
            )
        }
        eventsByType.put(SchemaKeyEventTypeNames.fromKey(type)!!, predictedEventsList.toList())
    }

    if (topRecommendationsByType.isEmpty()) {
        return Triple(null, null, DiscoverEventsResponse(newEventMap, eventsByType))
    }

    val allPredictedNodesIds = topRecommendationsByType.values.flatten().map{ it.targetNodeId }
    val eventPropNodes = eventNodesByType.values.toList()

    val (neighborNodes, neighborEdges) = buildGraphContext(
        repository = eventRepository,
        predictedNodeIds = allPredictedNodesIds,
        extraNodes = eventPropNodes,
        addSuggestionEdges = { edgeSet, nodeSet ->
            for (id in eventNodesByType.map { it.value.id }) {
                for ((type, recs) in topRecommendationsByType) {
                    for (rec in recs) {
                        val relationType = "Suggest-$type"
                        val newEdge = EventEdgeEntity(
                            id = -1L,
                            firstNodeId = id,
                            secondNodeId = rec.targetNodeId,
                            edgeType = relationType
                        )
                        edgeSet.add(newEdge)
                    }
                }
            }
        }
    )

    return Triple(
        neighborNodes.toList(),
        neighborEdges.toList(),
        DiscoverEventsResponse(newEventMap, eventsByType)
    )
}

/* -------------------------------------------------
    Function 2: Predict Missing Properties
------------------------------------------------- */


/* -------------------------------------------------
    Function 3: Detecting Patterns in data
------------------------------------------------- */
fun findPatterns(
    eventRepository: EventRepository,
    simMatrix: Map<Pair<Long, Long>, Float>
) : PatternFindingResponse {

    // 2. Build similarity graph (adjacency list)
    val threshold = 0.4f
    val similarityGraph = mutableMapOf<Long, MutableSet<Long>>()

    val allKeyNodeIds = eventRepository.getAllEventNodes().map { it.id }

    for ((pair, score) in simMatrix) {
        val (id1, id2) = pair
        if (score >= threshold  && id1 in allKeyNodeIds && id2 in allKeyNodeIds ) {
            similarityGraph.getOrPut(id1) { mutableSetOf() }.add(id2)
            similarityGraph.getOrPut(id2) { mutableSetOf() }.add(id1)
        }
    }

    // 3. Find connected components
    val visited = mutableSetOf<Long>()
    val components = mutableListOf<Set<Long>>()

    for (node in similarityGraph.keys) {
        if (node in visited) continue

        val component = mutableSetOf<Long>()
        val queue = ArrayDeque<Long>()
        queue.add(node)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current in visited) continue
            visited.add(current)
            component.add(current)
            similarityGraph[current]?.let { neighbors ->
                queue.addAll(neighbors)
            }
        }

        components.add(component)
    }

    // 4. Summarize each component
    val patternFindingResponse = mutableListOf<List<KeyNode>>()

    val filteredComponents = components.filter { it.size > 1 }
    for ((i, group) in filteredComponents.withIndex()) {
        val keyNodesList = mutableListOf<KeyNode>()
        for (nodeId in group) {
             val targetNode = eventRepository.getEventNodeById(nodeId)
             val targetNodeProps = eventRepository.getNeighborsOfEventNodeById(nodeId)
                 .filter { it.type in SchemaPropertyNodes }.associate { it.type to it.name}
             val keyNode = KeyNode(
                 nodeName = targetNode!!.name,
                 nodeDescription = targetNode.description,
                 nodeProperties = targetNodeProps
             )
             keyNodesList.add(keyNode)
        }
        patternFindingResponse.add(keyNodesList)

        println("Pattern #$i: Nodes=$keyNodesList")
    }

    return PatternFindingResponse(patternFindingResponse)
}

/* -------------------------------------------------
    Function 4: Detecting Same Event / Fusing Events
------------------------------------------------- */
suspend fun detectReplicateInput(
    newEventMap: Map<String, String>,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    ratioThreshold: Float = 0.8f,
    similarityThreshold: Float = 0.9f
) : Pair<EventNodeEntity?, ReplicaDetectionResponse> {

    val inputEmbeddings = newEventMap.entries.associate { (type, value) ->
        type to embeddingRepository.getTextEmbeddings(value)
    }
    val keyEventType = inputEmbeddings.keys.intersect(SchemaKeyEventTypeNames.entries)

    val allNodes = eventRepository.getAllEventNodes()

    val allKeyNodes = if (keyEventType.isNotEmpty()) {
        allNodes.filter { it.type in keyEventType }
    } else {
        allNodes.filter { it.type in SchemaKeyNodes }
    }

    val matchingCandidates = mutableListOf<SimilarEvent>()
    val similarNodesList = mutableListOf<Pair<EventNodeEntity, Float>>()

    for (candidate in allKeyNodes) {
        val propertySimilarities = mutableMapOf<String, Float>()
        var matchingCount = 0
        var numProperties = 0

        for (propertyType in SchemaPropertyNodes) {
            val candidatePropertyNode = eventRepository.getNeighborsOfEventNodeById(candidate.id)
                .firstOrNull { it.type == propertyType }

            if (candidatePropertyNode!= null) {
                numProperties++

                // If input also has the property
                if (inputEmbeddings[propertyType] != null) {
                    val similarity = embeddingRepository.cosineDistance(
                        candidatePropertyNode.embedding!!, inputEmbeddings[propertyType]!!
                    )
                    propertySimilarities.put(propertyType, similarity)

                    if (similarity >= similarityThreshold) {
                        matchingCount++
                    }
                }
            }
        }

        if (matchingCount == 0) continue

        val ratio = matchingCount.toFloat() / numProperties
        if (ratio >= ratioThreshold) {
            matchingCandidates.add(
                SimilarEvent(
                    eventName = candidate.name,
                    propertySimilarities = propertySimilarities,
                    similarityRatio = ratio,
                    averageSimilarityScore = propertySimilarities.map { it.value }.average().toFloat()
                )
            )
            similarNodesList.add(candidate to ratio)
        }
    }

    // Determine if any exceed threshold
    val duplicateNode = similarNodesList.maxByOrNull { it.second }
    val isLikelyDuplicate = matchingCandidates.any { it.averageSimilarityScore >= similarityThreshold }

//    val predictedNodeIds = similarNodesList.map { it.first.id }

//    val (neighborNodes, neighborEdges) = buildGraphContext(
//        repository = repository,
//        predictedNodeIds = predictedNodeIds,
//        addSuggestionEdges = { _, _ -> }
//    )

    // Build the response
    return Pair(
        duplicateNode?.first,
        ReplicaDetectionResponse(
            inputEvent = newEventMap,
            topSimilarEvents = matchingCandidates.sortedWith(
                compareByDescending<SimilarEvent> { it.similarityRatio }
                    .thenByDescending { it.averageSimilarityScore }),
            isLikelyDuplicate = isLikelyDuplicate
        )
    )
}

