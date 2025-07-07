package com.example.graphapp.data.schema

import android.util.Log
import com.example.graphapp.data.GraphRepository
import com.example.graphapp.data.api.AnomalyDetectionResponse
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
import com.example.graphapp.data.api.PropertyAgreement
import com.example.graphapp.data.api.ProvideRecommendationsResponse
import com.example.graphapp.data.api.Recommendation
import com.example.graphapp.data.api.SimilarEvent
import com.example.graphapp.data.schema.GraphSchema.keyNodes
import com.example.graphapp.data.schema.GraphSchema.propertyNodes
import com.example.graphdb.Edge
import com.example.graphdb.Node
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

/* -------------------------------------------------
    Function 1: Predict missing properties
------------------------------------------------- */
fun predictMissingProperties(
    repository: GraphRepository
): Pair<List<Edge>, PredictMissingPropertiesResponse> {

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
        val predictedPropertiesMap = mutableMapOf<String, Triple<Node, Float, String>>()

        // Iterate over the missing properties
        for (prop in missingProps) {

            // Collect candidates of same node type with this property
            val candidates = allNodes.filter { candidate ->
                candidate.id != node.id && candidate.type == node.type && repository
                    .getNeighborsOfNodeById(candidate.id)
                    .any { neighbour -> neighbour.type == prop }
            }

            // keyNode -> propNode, simValue
            val scoreByValue = mutableMapOf<Node, Pair<Node, Float>>()

            for (candidate in candidates) {
                val simScore = computeWeightedSim(
                    targetId = node.id,
                    candidateId = candidate.id,
                    repository = repository
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
    val newEdges = mutableListOf<Edge>()

    for (prediction in predictionsList) {
        val targetNodeId = prediction.nodeId
        for ((prop, sourceNodeName) in prediction.predictedProperties) {
            val relationType = "Suggest-$prop"
            val newEdge = Edge(
                id = -1L,
                fromNode = repository.findNodeByNameAndType(sourceNodeName, prop),
                toNode = targetNodeId,
                relationType = relationType
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
    repository: GraphRepository,
    noKeyNode: Boolean
) : Triple<List<Node>, List<Edge>, EventRecommendationResult> {

    val allNodes = repository.getAllNodes()
    val allNodesByType = allNodes
        .filter { it.type in (keyNodes) } // Exclude description nodes
        .groupBy { it.type }

    // Compute similarity of each candidate to all event nodes
    val eventNodeIds = newEventMap.entries.map { (type, value) ->
        repository.findNodeByNameAndType(value, type)
    }.toSet()

    // For each key node type, compute top 2
    val topRecommendationsByType = mutableMapOf<String, List<Pair<Node, Float>>>()

    for ((type, candidates) in allNodesByType) {
        val scoresForType = mutableListOf<Pair<Node, Float>>()

        for (candidate in candidates) {
            if (candidate.id in eventNodeIds) continue // skip the event key nodes themselves
            val avg = eventNodeIds.map { eventId ->
                val s = computeWeightedSim(
                    targetId = candidate.id,
                    candidateId = eventId,
                    repository = repository
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
    val neighborNodes = mutableListOf<Node>()
    val neighborEdges = mutableListOf<Edge>()

    val eventKeyIds = newEventMap
        .filter { (type, value) -> value.isNotBlank() && type in (keyNodes + propertyNodes) }
        .mapNotNull { (type, value) ->
            repository.findNodeByNameAndType(value, type)
        }
    val allPredictedNodes = topRecommendationsByType.values.flatten().map { it.first.id }

    for (id in (allPredictedNodes + eventKeyIds)) {
        val nodes = repository.getNeighborsOfNodeById(id)
        for (n in nodes) {
            val edges = repository.getEdgeBetweenNodes(id, n.id)
            neighborEdges.addAll(edges.filter { it !in neighborEdges })
        }
        neighborNodes.addAll(nodes.filter { it !in neighborNodes })
        repository.findNodeById(id)?.let { node ->
            if (node !in neighborNodes) {
                neighborNodes.add(node)
            }
        }
    }

    for (id in eventKeyIds) {
        for ((type, recs) in topRecommendationsByType) {
            for ((node, _) in recs) {
                val relationType = "Suggest-$type"
                val newEdge = Edge(
                    id = -1L,
                    fromNode = id,
                    toNode = node.id,
                    relationType = relationType
                )
                neighborEdges.add(newEdge)
            }
        }
    }

    return if (noKeyNode) {
        Triple(
            neighborNodes,
            neighborEdges,
            EventRecommendationResult.PropertyToEventRec(DiscoverEventsResponse(newEventMap, eventsByType))
        )
    } else {
        Triple(
            neighborNodes,
            neighborEdges,
            EventRecommendationResult.EventToEventRec(ProvideRecommendationsResponse(newEventMap, recList))
        )
    }
}

/* -------------------------------------------------
    Function 4: Pattern Recognition
------------------------------------------------- */
fun findPatterns(
    repository: GraphRepository
) : PatternFindingResponse {
    // 1. Compute similarity matrix
    val simMatrix = initialiseSimilarityMatrix(repository)

    // 2. Build similarity graph (adjacency list)
    val threshold = 0.4f
    val similarityGraph = mutableMapOf<Long, MutableSet<Long>>()

    val allKeyNodeIds = repository.getAllKeyNodeIds()

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
             val targetNode = repository.findNodeById(nodeId)
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
    Function 5: Anomaly Detection
------------------------------------------------- */
fun detectInputAnomaly(
    newEventMap: Map<String, String>,
    repository: GraphRepository
) : AnomalyDetectionResponse {
    val simMatrix = initialiseSimilarityMatrix(repository)

    val allNodes = repository.getAllNodes()
    val allKeyNodes = allNodes.filter { it.type in (keyNodes) }

    val eventNodeIds = newEventMap.entries.map { (type, value) ->
        repository.findNodeByNameAndType(value, type)
    }.toSet()

    // For each key node type, compute top 3
    val nodesWithSimilarityList = mutableListOf<Pair<Node, Float>>()

    for (candidate in allKeyNodes) {
        if (candidate.id in eventNodeIds) continue // skip the event key nodes themselves

        val avg = eventNodeIds.map { eventId ->
            val s = computeWeightedSim(
                targetId = candidate.id,
                candidateId = eventId,
                repository = repository
            )
            s
        }.average().toFloat()
        nodesWithSimilarityList.add(candidate to avg)
    }

    val mostSimilarNodes = nodesWithSimilarityList
        .sortedByDescending { it.second }
        .take(3)

    // For each property type, compute weighted agreement
    val propertyAnalyses = mutableListOf<PropertyAgreement>()
    val thresholdAgreement = 0.4f // can tune
    var flaggedProperties = 0

    for (propertyType in propertyNodes) {
        // 1. Get the candidate event's property node id
        val candidatePropertyNodeId = newEventMap[propertyType]?.let { propValue ->
            repository.findNodeByNameAndType(propValue, propertyType)
        }

        if (candidatePropertyNodeId == null) {
            continue
        }

        // 2. Collect weighted votes from similar nodes
        val weightedVotes = mutableMapOf<Long, Float>()
        for ((similarNode, simScore) in mostSimilarNodes) {
            // For each similar node, get its property node
            val neighbor = repository.getNeighborsOfNodeById(similarNode.id)
                .firstOrNull { it.type == propertyType }
            if (neighbor != null) {
                weightedVotes[neighbor.id] =
                    weightedVotes.getOrDefault(neighbor.id, 0f) + simScore
            }
        }

        // 3. Compute agreement
        val totalWeight = weightedVotes.values.sum()
        val agreeingWeight = weightedVotes[candidatePropertyNodeId] ?: 0f
        val agreement = if (totalWeight > 0f) agreeingWeight / totalWeight else 0f

        val isAnomalous = agreement < thresholdAgreement
        if (isAnomalous) flaggedProperties++

        // Add to analyses
        propertyAnalyses.add(
            PropertyAgreement(
                propertyType = propertyType,
                candidateValue = repository.findNodeById(candidatePropertyNodeId)?.name ?: "Unknown",
                isAnomalous = isAnomalous
            )
        )
    }
    // Build top similar event details
    val topSimilarEvents = mostSimilarNodes.map { (node, simScore) ->
        val propertyValues = propertyNodes.associateWith { propertyType ->
            repository.getNeighborsOfNodeById(node.id)
                .firstOrNull { it.type == propertyType }
                ?.name ?: "Unknown"
        }
        SimilarEvent(
            eventName = node.name,
            propertyValues = propertyValues
        )
    }

    // Build the response
    return AnomalyDetectionResponse(
        inputEvent = newEventMap,
        overallAnomaly = flaggedProperties >= 2,
        flaggedPropertyCount = flaggedProperties,
        propertyAnalyses = propertyAnalyses,
        topSimilarEvents = topSimilarEvents
    )
}

//fun findExistingRelations(
//    repository: GraphRepository
//) : List<Edge> {
//
//    val simMatrix = initialiseSimilarityMatrix(repository)
//
//    // Collect all candidate key nodes, grouped by type
//    val allNodes = repository.getAllNodes()
//    val relevantNodes = allNodes
//        .filter { it.type in (keyNodes) }
////        .groupBy { it.type }
//
//    val allScores = mutableListOf<Triple<Long, Long, Float>>()
//
//    for (node in relevantNodes) {
//        for (other in relevantNodes) {
//            if ( node.id == other.id ) continue
//
//            val (small, large) = if (node.id < other.id) node.id to other.id else other.id to node.id
//            val s = simMatrix.getOrDefault(small to large, 0f)
//
//            if (s>=0.20f) continue // skip zero similarity
//            allScores.add(Triple(small, large, s))
//        }
//    }
//
//    val topScoringRelations = allScores.toSet().toList()
//        .sortedByDescending { it.third }
//        .take(5)
//
//    Log.d("topScoringRelations", "Here: $topScoringRelations")
//
//    val floats = allScores.map { it.third }
//    val sorted = floats.sorted()
//    fun percentile(p: Double): Float {
//        if (sorted.isEmpty()) return 0f
//        val index = ((p / 100.0) * (sorted.size - 1)).toInt()
//        return sorted[index]
//    }
//
//    Log.d("Distribution", "25th percentile: ${percentile(25.0)}")
//    Log.d("Distribution", "50th percentile: ${percentile(50.0)}")
//    Log.d("Distribution", "75th percentile: ${percentile(75.0)}")
//
//    val newEdges = mutableListOf<Edge>()
//
//    for ((id1, id2, _) in topScoringRelations) {
//        val relationType = "Related"
//        val newEdge = Edge(
//            id = -1L,
//            fromNode = id1,
//            toNode = id2,
//            relationType = relationType
//        )
//        newEdges.add(newEdge)
//    }
//    return newEdges
//}
