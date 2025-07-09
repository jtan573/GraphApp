package com.example.graphapp.data.schema

import android.util.Log
import androidx.compose.runtime.simulateHotReload
import com.example.graphapp.data.GraphRepository
import com.example.graphapp.data.VectorRepository
import com.example.graphapp.data.api.DiscoverEventsResponse
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.api.EventRecommendationResult
import com.example.graphapp.data.api.KeyNode
import com.example.graphapp.data.api.NodeDetails
import com.example.graphapp.data.api.PatternFindingResponse
import com.example.graphapp.data.api.PredictMissingProperties
import com.example.graphapp.data.api.PredictMissingPropertiesResponse
import com.example.graphapp.data.api.PredictedEventByType
import com.example.graphapp.data.api.PredictedProperty
import com.example.graphapp.data.api.ProvideRecommendationsResponse
import com.example.graphapp.data.api.Recommendation
import com.example.graphapp.data.api.ReplicaDetectionResponse
import com.example.graphapp.data.api.SimilarEvent
import com.example.graphapp.data.local.EdgeEntity
import com.example.graphapp.data.local.NodeEntity
import com.example.graphapp.data.schema.GraphSchema.keyNodes
import com.example.graphapp.data.schema.GraphSchema.propertyNodes
//import com.example.graphdb.Edge
//import com.example.graphdb.Node
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

/* -------------------------------------------------
    Function 1: Predict missing properties
------------------------------------------------- */
fun predictMissingProperties(
//    repository: GraphRepository,
    repository: VectorRepository,
): Pair<List<EdgeEntity>, PredictMissingPropertiesResponse> {

    val predictionsList = mutableListOf<PredictMissingProperties>()

    val allNodes = repository.getAllNodes()
    val allKeyNodes = allNodes.filter { it.type in keyNodes }

    for (node in allKeyNodes) {

        val existingPropertiesNodes = repository.getNeighborsOfNodeById(node.id)
        val existingPropertyTypes = existingPropertiesNodes.map { it.type }
        val missingProps = propertyNodes.filter { prop ->
            prop !in existingPropertyTypes
        }

        if (missingProps.isEmpty()) continue

        var flag = false
        // missingPropType -> propNode, simScore, keyNodeName
        val predictedPropertiesMap = mutableMapOf<String, Triple<NodeEntity, Float, String>>()

        // Iterate over the missing properties
        for (prop in missingProps) {

            // Collect candidates of same node type with this property
            val candidates = allNodes.filter { candidate ->
                candidate.id != node.id && candidate.type == node.type && repository
                    .getNeighborsOfNodeById(candidate.id)
                    .any { neighbour -> neighbour.type == prop }
            }

            // keyNode -> propNode, simValue
            val scoreByValue = mutableMapOf<NodeEntity, Pair<NodeEntity, Float>>()

            val simMatrix = initialiseSemanticSimilarityMatrix(repository)
            for (candidate in candidates) {
                val simScore = computeWeightedSim(
                    targetId = node.id,
                    candidateId = candidate.id,
                    repository = repository,
                    similarityMatrix = simMatrix
                )

                // Retrieve the property value of this candidate
                val propNode = repository
                    .getNeighborsOfNodeById(candidate.id)
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

            // Build PredictMissingProperties object
            // Prepare existingProperties Map<String, String> for NodeDetails
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
    val newEdges = mutableListOf<EdgeEntity>()

    for (prediction in predictionsList) {
        val targetNodeId = prediction.nodeId
        for ((prop, sourceNodeName) in prediction.predictedProperties) {
            val relationType = "Suggest-$prop"
            val newEdge = EdgeEntity(
                id = -1L,
                fromId = repository.getNodeByNameAndType(sourceNodeName, prop)!!.id,
                toId = targetNodeId,
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
fun recommendOnInput (
    newEventMap: Map<String, String>,
    repository: VectorRepository,
    noKeyNode: Boolean,
    similarityMatrix: Map<Pair<Long, Long>, Float>
) : Triple<List<NodeEntity>, List<EdgeEntity>, EventRecommendationResult> {

    val allNodes = repository.getAllNodes()
    val allNodesByType = allNodes
        .filter { it.type in (keyNodes) }
        .groupBy { it.type }

    // Compute similarity of each candidate to all event nodes
    val eventNodeIds = newEventMap.entries.map { (type, value) ->
        repository.getNodeByNameAndType(value, type)!!.id
    }.toSet()

    // For each key node type, compute top 2
    val topRecommendationsByType = mutableMapOf<String, List<Pair<NodeEntity, Float>>>()
//    val simMatrix = initialiseSemanticSimilarityMatrix(repository)

    for ((type, candidates) in allNodesByType) {
        val scoresForType = mutableListOf<Pair<NodeEntity, Float>>()

        for (candidate in candidates) {
            if (candidate.id in eventNodeIds) continue
            val avg = eventNodeIds.map { eventId ->
                val s = computeWeightedSim(
                    targetId = candidate.id,
                    candidateId = eventId,
                    repository = repository,
                    similarityMatrix = similarityMatrix
                )
                s
            }.average().toFloat()
            scoresForType.add(candidate to avg)
        }

        val topForType = scoresForType
            .sortedByDescending { it.second }
            .take(2)

        topRecommendationsByType[type] = topForType
    }

    // Create different format API response for "predict" and "discover" functions
    val recList = mutableListOf<Recommendation>()
    val eventsByType = mutableListOf<PredictedEventByType>()

    if (noKeyNode) {
        // 1. If discovering events from property
        for ((type, recs) in topRecommendationsByType) {
            val predictedEventsList = mutableListOf<EventDetails>()
            for (rec in recs) {
                val neighbourProps = repository.getNeighborsOfNodeById(rec.first.id)
                    .filter { it.type in propertyNodes }
                    .associate { it.type to it.name }
                predictedEventsList.add(
                    EventDetails(
                        eventName = rec.first.name,
                        eventProperties = neighbourProps,
                        simScore = rec.second
                    )
                )
            }
            eventsByType.add(
                PredictedEventByType(
                    eventType = type,
                    eventList = predictedEventsList
                )
            )
        }
    } else {
        // 2. If recommending events for events
        for ((type, recs) in topRecommendationsByType) {
            val recsByTypeList = mutableListOf<Pair<String, Float>>()
            for (rec in recs) {
                recsByTypeList.add(rec.first.name to rec.second)
            }
            recList.add(
                Recommendation(
                    recType = type,
                    recItems = recsByTypeList
                )
            )
        }
    }

    // Creating filtered graph
    val neighborNodes = mutableSetOf<NodeEntity>()
    val neighborEdges = mutableSetOf<EdgeEntity>()

    val eventKeyIds = newEventMap
        .filter { (type, value) -> value.isNotBlank() && type in (keyNodes + propertyNodes) }
        .mapNotNull { (type, value) ->
            repository.getNodeByNameAndType(value, type)!!.id
        }
    val allPredictedNodes = topRecommendationsByType.values.flatten().map { it.first.id }

    for (id in (allPredictedNodes + eventKeyIds)) {
        val nodes = repository.getNeighborsOfNodeById(id)

        for (n in nodes) {
            Log.d("CHECK NODES", "${n.id}, $id")
            if (id == n.id) continue
            val edge = repository.getEdgeBetweenNodes(id, n.id)
            neighborEdges.add(edge)
        }
        neighborNodes.addAll(nodes)
        neighborNodes.add(repository.getNodeById(id)!!)

        val neighbourIds = neighborNodes.map { it.id }
        Log.d("CHECK NODES", "$neighbourIds")
    }

    for (id in eventKeyIds) {
        for ((type, recs) in topRecommendationsByType) {
            for ((node, _) in recs) {
                val relationType = "Suggest-$type"
                val newEdge = EdgeEntity(
                    id = -1L,
                    fromId = id,
                    toId = node.id,
                    edgeType = relationType
                )
                neighborEdges.add(newEdge)
            }
        }
    }

    return if (noKeyNode) {
        Triple(
            neighborNodes.toList(),
            neighborEdges.toList(),
            EventRecommendationResult.PropertyToEventRec(DiscoverEventsResponse(newEventMap, eventsByType))
        )
    } else {
        Triple(
            neighborNodes.toList(),
            neighborEdges.toList(),
            EventRecommendationResult.EventToEventRec(ProvideRecommendationsResponse(newEventMap, recList))
        )
    }
}

/* -------------------------------------------------
    Function 4: Pattern Recognition
------------------------------------------------- */
fun findPatterns(
    repository: VectorRepository
) : PatternFindingResponse {
    // 1. Compute similarity matrix
    val simMatrix = initialiseSemanticSimilarityMatrix(repository)

    // 2. Build similarity graph (adjacency list)
    val threshold = 0.4f
    val similarityGraph = mutableMapOf<Long, MutableSet<Long>>()

    val allKeyNodeIds = repository.getAllNodes().map { it.id }

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
             val targetNode = repository.getNodeById(nodeId)
             val targetNodeProps = repository.getNeighborsOfNodeById(nodeId)
                 .filter { it.type in propertyNodes }.associate { it.type to it.name}
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
fun detectReplicateInput(
    newEventMap: Map<String, String>,
    repository: VectorRepository,
    ratioThreshold: Float = 0.8f,
    similarityThreshold: Float = 0.8f
) : ReplicaDetectionResponse {

    val allNodes = repository.getAllNodes()
    val allKeyNodes = allNodes.filter { it.type in (keyNodes) }

    val inputEmbeddings = newEventMap.entries.associate { (type, value) ->
        type to repository.getNodeByNameAndType(value, type)
    }

    val matchingCandidates = mutableListOf<SimilarEvent>()

    for (candidate in allKeyNodes) {
        val propertySimilarities = mutableMapOf<String, Float>()
        var matchingCount = 0
        var numProperties = propertyNodes.size

        for (propertyType in propertyNodes) {
            val candidatePropertyNode = repository.getNeighborsOfNodeById(candidate.id)
                .firstOrNull { it.type == propertyType }

            if (candidatePropertyNode!= null &&
                candidatePropertyNode.embedding != null &&
                inputEmbeddings[propertyType] != null
            ) {
                val similarity = repository.cosineDistance(
                    candidatePropertyNode.embedding!!, inputEmbeddings[propertyType]!!.embedding!!
                )
                propertySimilarities.put(propertyType, similarity)

                if (similarity >= similarityThreshold) {
                    matchingCount++
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
        }
    }

    // Determine if any exceed threshold
    val isLikelyDuplicate = matchingCandidates.any { it.averageSimilarityScore >= similarityThreshold }

    // Build the response
    return ReplicaDetectionResponse(
        inputEvent = newEventMap,
        topSimilarEvents = matchingCandidates.sortedWith(
            compareByDescending<SimilarEvent> { it.similarityRatio }
                .thenByDescending { it.averageSimilarityScore }),
        isLikelyDuplicate = isLikelyDuplicate
    )
}

