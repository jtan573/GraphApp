package com.example.graphapp.backend.core

import android.util.Log
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.api.DiscoverEventsResponse
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.api.KeyNode
import com.example.graphapp.data.api.NodeDetails
import com.example.graphapp.data.api.PatternFindingResponse
import com.example.graphapp.data.api.PredictMissingProperties
import com.example.graphapp.data.api.PredictMissingPropertiesResponse
import com.example.graphapp.data.api.PredictedProperty
import com.example.graphapp.data.api.ReplicaDetectionResponse
import com.example.graphapp.data.api.SimilarEvent
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.backend.dto.GraphSchema.SchemaKeyNodes
import com.example.graphapp.backend.dto.GraphSchema.SchemaOtherNodes
import com.example.graphapp.backend.dto.GraphSchema.SchemaPropertyNodes
//import com.example.graphdb.Edge
//import com.example.graphdb.Node
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

/* -------------------------------------------------
    Function 1: Predict missing properties
------------------------------------------------- */
fun predictMissingProperties(
    eventRepository: EventRepository,
    simMatrix: Map<Pair<Long, Long>, Float>
): Pair<List<EventEdgeEntity>, PredictMissingPropertiesResponse> {

    val predictionsList = mutableListOf<PredictMissingProperties>()

    val allNodes = eventRepository.getAllEventNodes()
    val allKeyNodes = allNodes.filter { it.type in SchemaKeyNodes }

    for (node in allKeyNodes) {

        val existingPropertiesNodes = eventRepository.getNeighborsOfEventNodeById(node.id)
        val existingPropertyTypes = existingPropertiesNodes.map { it.type }
        val missingProps = SchemaPropertyNodes.filter { prop ->
            prop !in existingPropertyTypes
        }

        if (missingProps.isEmpty()) continue

        var flag = false
        // missingPropType -> propNode, simScore, keyNodeName
        val predictedPropertiesMap = mutableMapOf<String, Triple<EventNodeEntity, Float, String>>()

        // Iterate over the missing properties
        for (prop in missingProps) {

            // Collect candidates of same node type with this property
            val candidates = allNodes.filter { candidate ->
                candidate.id != node.id && candidate.type == node.type && eventRepository
                    .getNeighborsOfEventNodeById(candidate.id)
                    .any { neighbour -> neighbour.type == prop }
            }

            // keyNode -> propNode, simValue
            val scoreByValue = mutableMapOf<EventNodeEntity, Pair<EventNodeEntity, Float>>()

            for (candidate in candidates) {
                val simScore = computeWeightedSim(
                    targetId = node.id,
                    candidateId = candidate.id,
                    repository = eventRepository,
                    similarityMatrix = simMatrix
                )

                // Retrieve the property value of this candidate
                val propNode = eventRepository
                    .getNeighborsOfEventNodeById(candidate.id)
                    .firstNotNullOf { neighbour ->
                        if (neighbour.type == prop) { neighbour } else null
                    }

                scoreByValue[candidate] = propNode to simScore
            }

            if (scoreByValue.isNotEmpty()) {
                val (simNode, simProp) = scoreByValue.maxByOrNull { it.value.second }!!
                predictedPropertiesMap[prop] = Triple(simProp.first, simProp.second, simNode.name)
                flag = true
            }
        }

        if (flag) {
            val existingPropertiesMap = existingPropertiesNodes.associate { n ->
                n.type to n.name
            }

            val predictedPropList = mutableListOf<PredictedProperty>()
            for ((pType, pVal) in predictedPropertiesMap) {
                predictedPropList.add(
                    PredictedProperty(
                        propertyType = pType,
                        propertyValue = pVal.first.name,
                        simScore = pVal.second,
                        mostSimilarKeyNode = pVal.third
                    )
                )
            }

            val predictMissing = PredictMissingProperties(
                nodeId = node.id,
                nodeDetails = NodeDetails(
                    type = node.type,
                    name = node.name,
                    description = node.description,
                    existingProperties = existingPropertiesMap
                ),
                predictedProperties = predictedPropList
            )
            predictionsList.add(predictMissing)
        }
    }

    // Creating Edges for UI
    val newEdges = mutableListOf<EventEdgeEntity>()

    for (prediction in predictionsList) {
        val targetNodeId = prediction.nodeId
        for ((prop, sourceNodeName) in prediction.predictedProperties) {
            val relationType = "Suggest-$prop"
            val newEdge = EventEdgeEntity(
                id = -1L,
                firstNodeId = eventRepository.getEventNodeByNameAndType(sourceNodeName, prop)!!.id,
                secondNodeId = targetNodeId,
                edgeType = relationType
            )
            newEdges.add(newEdge)
        }
    }

    return newEdges to PredictMissingPropertiesResponse(predictionsList)
}

/* -------------------------------------------------
    Function 2: Provide event recommendations on input event
    Function 3: Provide event recommendations on input property
------------------------------------------------- */
//fun recommendEventForEvent(
//    newEventMap: Map<String, String>,
//    eventRepository: EventRepository,
//    simMatrix: Map<Pair<Long, Long>, Float>,
//    newEventNodes: List<EventNodeEntity>,
//    queryKey: String? = null,
//    isQuery: Boolean
//) : Triple<List<EventNodeEntity>, List<EventEdgeEntity>, ProvideRecommendationsResponse> {
//
//    val inputKeyNode = newEventNodes.single { it.type in SchemaKeyNodes }
//
//    var topRecommendationsByType = mutableMapOf<String, MutableList<EventNodeEntity>>()
//
//    // Check cache
//    val cachedRecommendations = loadCachedRecommendations(inputKeyNode, eventRepository, queryKey)
//    topRecommendationsByType = if (cachedRecommendations.isNotEmpty()) {
//        cachedRecommendations
//    } else {
//        computeTopRecommendations(
//            inputKeyNode = inputKeyNode,
//            repository = eventRepository,
//            simMatrix = simMatrix,
//            queryKey = queryKey
//        )
//    }
//
//    // Create API response format
//    val recList = mutableMapOf<String, List<String>>()
//
//    for ((type, recs) in topRecommendationsByType) {
//        val recsByTypeList = mutableListOf<String>()
//        for (rec in recs) { recsByTypeList.add(rec.name) }
//        recList.put(type, recsByTypeList)
//    }
//
//    val allPredictedNodeIds = topRecommendationsByType.values.flatten().map { it.id }
//
//    val (neighborNodes, neighborEdges) = buildGraphContext(
//        repository = eventRepository,
//        predictedNodeIds = allPredictedNodeIds + inputKeyNode.id,
//        addSuggestionEdges = { edgeSet, nodeSet ->
//            for ((type, recs) in topRecommendationsByType) {
//                for (node in recs) {
//                    val relationType = "Suggest-$type"
//                    val newEdge = EventEdgeEntity(
//                        id = -1L,
//                        firstNodeId = node.id,
//                        secondNodeId = inputKeyNode.id,
//                        edgeType = relationType
//                    )
//                    edgeSet.add(newEdge)
//                }
//            }
//        }
//    )
//
//    // If not query: Update cache of input node
//    if (!isQuery) {
//        for ((type, recNodeList) in topRecommendationsByType) {
//            inputKeyNode.cachedNodeIds.getOrPut(type) { mutableListOf() }
//                .addAll(recNodeList.map { it.id })
//        }
//        Log.d("Cached Node", "${inputKeyNode.cachedNodeIds}")
//    } else {
//        // Add input nodes to list too
//        neighborNodes.addAll(newEventNodes)
//        for (propNode in newEventNodes) {
//            if (propNode.type in SchemaKeyNodes) continue
//            val relationType = SchemaEdgeLabels["${propNode.type}-${inputKeyNode.type}"]
//            val newEdge = EventEdgeEntity(
//                id = -1L,
//                firstNodeId = propNode.id,
//                secondNodeId = inputKeyNode.id,
//                edgeType = relationType
//            )
//            neighborEdges.add(newEdge)
//        }
//    }
//
//    return Triple(
//        neighborNodes.toList(),
//        neighborEdges.toList(),
//        ProvideRecommendationsResponse(newEventMap, recList)
//    )
//}

suspend fun recommendEventsForProps (
    newEventMap: Map<String, String>,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    queryKey: String? = null,
    getTopThreeResultsOnly: Boolean = true,
    customThreshold: Float = 0.0f,
    activeNodesOnly: Boolean
) : Triple<List<EventNodeEntity>?, List<EventEdgeEntity>?, DiscoverEventsResponse> {

    // Compute similarity of each candidate to event nodes
    var eventNodesByType = mutableMapOf<String, EventNodeEntity>()
    newEventMap.entries.map { (type, value) ->
        val nodeExist = eventRepository.getEventNodeByNameAndType(value, type)
        if (nodeExist != null) {
            eventNodesByType[type] = nodeExist
        } else {
            eventNodesByType[type] = eventRepository.getTemporaryEventNode(value, type, embeddingRepository)
        }
    }

    // For each key node type, compute top 3
    val topRecommendationsByType = computeSemanticSimilarEventsForProps(
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        newEventMap = eventNodesByType,
        queryKey = queryKey,
        getTopThreeResultsOnly = getTopThreeResultsOnly,
        threshold = customThreshold,
        activeNodesOnly = activeNodesOnly
    )
    Log.d("topRecommendationsByType","$topRecommendationsByType")

    val eventsByType = mutableMapOf<String, List<EventDetails>>()

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
        eventsByType.put(type, predictedEventsList.toList())
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
    Function 4: Pattern Recognition
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
    Function 5: Detecting Same Event / Fusing Events
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
    val keyEventType = inputEmbeddings.keys.intersect(SchemaKeyNodes)

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

