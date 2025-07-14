package com.example.graphapp.data.schema

import android.util.Log
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
import com.example.graphapp.data.schema.GraphSchema.SchemaEdgeLabels
import com.example.graphapp.data.schema.GraphSchema.SchemaKeyNodes
import com.example.graphapp.data.schema.GraphSchema.SchemaPropertyNodes
//import com.example.graphdb.Edge
//import com.example.graphdb.Node
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

/* -------------------------------------------------
    Function 1: Predict missing properties
------------------------------------------------- */
fun predictMissingProperties(
    repository: VectorRepository,
    simMatrix: Map<Pair<Long, Long>, Float>
): Pair<List<EdgeEntity>, PredictMissingPropertiesResponse> {

    val predictionsList = mutableListOf<PredictMissingProperties>()

    val allNodes = repository.getAllNodes()
    val allKeyNodes = allNodes.filter { it.type in SchemaKeyNodes }

    for (node in allKeyNodes) {

        val existingPropertiesNodes = repository.getNeighborsOfNodeById(node.id)
        val existingPropertyTypes = existingPropertiesNodes.map { it.type }
        val missingProps = SchemaPropertyNodes.filter { prop ->
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
                firstNodeId = repository.getNodeByNameAndType(sourceNodeName, prop)!!.id,
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
fun recommendEventForEvent(
    newEventMap: Map<String, String>,
    repository: VectorRepository,
    simMatrix: Map<Pair<Long, Long>, Float>,
    isQuery: Boolean,
    newEventNodes: List<NodeEntity>,
) : Triple<List<NodeEntity>, List<EdgeEntity>, EventRecommendationResult> {

    val inputKeyNode = newEventNodes.single { it.type in SchemaKeyNodes }

    val topRecommendationsByType = mutableMapOf<String, MutableList<NodeEntity>>()
    val start = System.currentTimeMillis()

    if (inputKeyNode.cachedNodeIds.isNotEmpty()) {
        // Check if there are cached similar nodes
        Log.d("CACHEDNODES", "Found cached nodes: ${inputKeyNode.cachedNodeIds}")

        for ((type, cacheIds) in inputKeyNode.cachedNodeIds) {
            for (id in cacheIds) {
                topRecommendationsByType.getOrPut(type) { mutableListOf() }.add(repository.getNodeById(id)!!)
            }
        }
        val end = System.currentTimeMillis()
        println("Time taken: ${end - start} ms")
    } else {
        // If cache is empty, then calculate similarity
        val allNodesOfEventType = repository.getAllNodes()
            .filter { it.type == inputKeyNode.type }

        val scoresForEventKeyType = mutableListOf<Pair<NodeEntity, Float>>()

        for (candidate in allNodesOfEventType) {
            if (candidate.id == inputKeyNode.id) continue

            val simScore = computeWeightedSim(
                targetId = candidate.id,
                candidateId = inputKeyNode.id,
                repository = repository,
                similarityMatrix = simMatrix
            ).toFloat()
            scoresForEventKeyType.add(candidate to simScore)
        }
        val topForType = scoresForEventKeyType.sortedByDescending { it.second }.take(3)

        // Compute Top Recommendations
        for ((simNode, _) in topForType) {

            topRecommendationsByType.getOrPut(simNode.type) { mutableListOf() }.add(simNode)

            val neighbourKeyNodes = repository.getNeighborsOfNodeById(simNode.id)
                .filter { it.type in SchemaKeyNodes }

            if (neighbourKeyNodes.isEmpty()) continue

            for (neighbour in neighbourKeyNodes) {
                topRecommendationsByType.getOrPut(neighbour.type) { mutableListOf() }.add(neighbour)
            }
        }
        val end = System.currentTimeMillis()
        println("Time taken: ${end - start} ms")
    }

    if (inputKeyNode.cachedNodeIds.isNotEmpty()) {
        // Check if there are cached similar nodes
        Log.d("CACHEDNODES", "Found cached nodes: ${inputKeyNode.cachedNodeIds}")

        for ((type, cacheIds) in inputKeyNode.cachedNodeIds) {
            for (id in cacheIds) {
                topRecommendationsByType.getOrPut(type) { mutableListOf() }.add(repository.getNodeById(id)!!)
            }
        }
        val end = System.currentTimeMillis()
        println("Time taken: ${end - start} ms")
    } else {
        // If cache is empty, then calculate similarity
        val allNodesOfEventType = repository.getAllNodes()
            .filter { it.type == inputKeyNode.type }

        val scoresForEventKeyType = mutableListOf<Pair<NodeEntity, Float>>()

        for (candidate in allNodesOfEventType) {
            if (candidate.id == inputKeyNode.id) continue

            val simScore = computeWeightedSim(
                targetId = candidate.id,
                candidateId = inputKeyNode.id,
                repository = repository,
                similarityMatrix = simMatrix
            ).toFloat()
            scoresForEventKeyType.add(candidate to simScore)
        }
        val topForType = scoresForEventKeyType.sortedByDescending { it.second }.take(3)

        // Compute Top Recommendations
        for ((simNode, _) in topForType) {

            topRecommendationsByType.getOrPut(simNode.type) { mutableListOf() }.add(simNode)

            val neighbourKeyNodes = repository.getNeighborsOfNodeById(simNode.id)
                .filter { it.type in SchemaKeyNodes }

            if (neighbourKeyNodes.isEmpty()) continue

            for (neighbour in neighbourKeyNodes) {
                topRecommendationsByType.getOrPut(neighbour.type) { mutableListOf() }.add(neighbour)
            }
        }
        val end = System.currentTimeMillis()
        println("Time taken: ${end - start} ms")
    }

    // Create API response format
    val recList = mutableListOf<Recommendation>()

    for ((type, recs) in topRecommendationsByType) {
        val recsByTypeList = mutableListOf<String>()
        for (rec in recs) { recsByTypeList.add(rec.name) }
        recList.add(Recommendation(recType = type, recItems = recsByTypeList))
    }

    // Creating filtered graph
    val neighborNodes = mutableSetOf<NodeEntity>()
    val neighborEdges = mutableSetOf<EdgeEntity>()

    val allPredictedNodes = topRecommendationsByType.values.flatten().map { it.id }
    for (id in (allPredictedNodes + inputKeyNode.id)) {
        val nodes = repository.getNeighborsOfNodeById(id)

        for (n in nodes) {
            if (id == n.id) continue
            val edge = repository.getEdgeBetweenNodes(id, n.id)
            neighborEdges.add(edge)
        }
        nodes.forEach { node ->
            if (neighborNodes.none { it.id == node.id }) { neighborNodes.add(node) }
        }
        val node = repository.getNodeById(id)
        if (node != null && neighborNodes.none { it.id == node.id }) {
            neighborNodes.add(node)
        }
    }

    for ((type, recs) in topRecommendationsByType) {
        for (node in recs) {
            val relationType = "Suggest-$type"
            val newEdge = EdgeEntity(id = -1L, firstNodeId = node.id, secondNodeId = inputKeyNode.id, edgeType = relationType)
            neighborEdges.add(newEdge)
        }
    }

    // If not query: Update cache of input node
    if (!isQuery) {
        for ((type, recNodeList) in topRecommendationsByType) {
            inputKeyNode.cachedNodeIds.getOrPut(type) { mutableListOf() }
                .addAll(recNodeList.map { it.id })
        }
        Log.d("Cached Node", "${inputKeyNode.cachedNodeIds}")
    } else {
        // Add input nodes to list too
        neighborNodes.addAll(newEventNodes)
        for (propNode in newEventNodes) {
            if (propNode.type in SchemaKeyNodes) continue
            val relationType = SchemaEdgeLabels["${propNode.type}-${inputKeyNode.type}"]
            val newEdge = EdgeEntity(id = -1L, firstNodeId = propNode.id, secondNodeId = inputKeyNode.id, edgeType = relationType)
            neighborEdges.add(newEdge)
        }
    }

    return Triple(
        neighborNodes.toList(),
        neighborEdges.toList(),
        EventRecommendationResult.EventToEventRec(ProvideRecommendationsResponse(newEventMap, recList))
    )
}

fun recommendEventsForProps (
    newEventMap: Map<String, String>,
    repository: VectorRepository,
    simMatrix: Map<Pair<Long, Long>, Float>,
//    isQuery: Boolean,
//    newEventNodes: List<NodeEntity>
) : Triple<List<NodeEntity>, List<EdgeEntity>, EventRecommendationResult> {

    val allNodesByType = repository.getAllNodes()
        .filter { it.type in SchemaKeyNodes }.groupBy { it.type }

    // Compute similarity of each candidate to event nodes
    var eventNodeIds = mutableListOf<Long>()
    newEventMap.entries.map { (type, value) ->
        eventNodeIds.add(repository.getNodeByNameAndType(value, type)!!.id)
    }

    // For each key node type, compute top 3
    val topRecommendationsByType = mutableMapOf<String, List<Pair<NodeEntity, Float>>>()

    for ((type, candidates) in allNodesByType) {
        val scoresForType = mutableListOf<Pair<NodeEntity, Float>>()

        for (candidate in candidates) {
            if (candidate.id in eventNodeIds) continue
            val avg = eventNodeIds.map { eventId ->
                val s = computeWeightedSim(
                    targetId = candidate.id,
                    candidateId = eventId,
                    repository = repository,
                    similarityMatrix = simMatrix
                )
                s
            }.average().toFloat()
            scoresForType.add(candidate to avg)
        }

        val topForType = scoresForType
            .sortedByDescending { it.second }
            .take(3)

        topRecommendationsByType[type] = topForType
    }

    // Create different format API response for "predict" and "discover" functions
    val eventsByType = mutableListOf<PredictedEventByType>()

    for ((type, recs) in topRecommendationsByType) {
        val predictedEventsList = mutableListOf<EventDetails>()
        for (rec in recs) {
            val neighbourProps = repository.getNeighborsOfNodeById(rec.first.id)
                .filter { it.type in SchemaPropertyNodes }
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

    // Creating filtered graph
    val neighborNodes = mutableSetOf<NodeEntity>()
    val neighborEdges = mutableSetOf<EdgeEntity>()

    val allPredictedNodes = topRecommendationsByType.values.flatten().map { it.first.id }

    for (id in (allPredictedNodes + eventNodeIds)) {
        val nodes = repository.getNeighborsOfNodeById(id)

        for (n in nodes) {
            if (id == n.id) continue
            val edge = repository.getEdgeBetweenNodes(id, n.id)
            neighborEdges.add(edge)
        }
        nodes.forEach { node ->
            if (neighborNodes.none { it.id == node.id }) {
                neighborNodes.add(node)
            }
        }

        val node = repository.getNodeById(id)
        if (node != null && neighborNodes.none { it.id == node.id }) {
            neighborNodes.add(node)
        }
    }

    for (id in eventNodeIds) {
        for ((type, recs) in topRecommendationsByType) {
            for ((node, _) in recs) {
                val relationType = "Suggest-$type"
                val newEdge = EdgeEntity(
                    id = -1L,
                    firstNodeId = id,
                    secondNodeId = node.id,
                    edgeType = relationType
                )
                neighborEdges.add(newEdge)
            }
        }
    }

    return Triple(
        neighborNodes.toList(),
        neighborEdges.toList(),
        EventRecommendationResult.PropertyToEventRec(DiscoverEventsResponse(newEventMap, eventsByType))
    )
}


/* -------------------------------------------------
    Function 4: Pattern Recognition
------------------------------------------------- */
fun findPatterns(
    repository: VectorRepository,
    simMatrix: Map<Pair<Long, Long>, Float>
) : PatternFindingResponse {

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
    repository: VectorRepository,
    ratioThreshold: Float = 0.8f,
    similarityThreshold: Float = 0.9f
) : Triple<Pair<List<NodeEntity>, List<EdgeEntity>>,
        NodeEntity?,
        ReplicaDetectionResponse> {

    val inputEmbeddings = newEventMap.entries.associate { (type, value) ->
        type to repository.getTextEmbeddings(value)
    }
    val keyEventType = inputEmbeddings.keys.intersect(SchemaKeyNodes)

    val allNodes = repository.getAllNodes()

    val allKeyNodes = if (keyEventType.isNotEmpty()) {
        allNodes.filter { it.type in keyEventType }
    } else {
        allNodes.filter { it.type in SchemaKeyNodes }
    }

    val matchingCandidates = mutableListOf<SimilarEvent>()
    val similarNodesList = mutableListOf<Pair<NodeEntity, Float>>()

    for (candidate in allKeyNodes) {
        val propertySimilarities = mutableMapOf<String, Float>()
        var matchingCount = 0
        var numProperties = 0

        for (propertyType in SchemaPropertyNodes) {
            val candidatePropertyNode = repository.getNeighborsOfNodeById(candidate.id)
                .firstOrNull { it.type == propertyType }

            if (candidatePropertyNode!= null) {
                numProperties++

                // If input also has the property
                if (inputEmbeddings[propertyType] != null) {
                    val similarity = repository.cosineDistance(
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

    // Creating filtered graph
    val neighborNodes = mutableSetOf<NodeEntity>()
    val neighborEdges = mutableSetOf<EdgeEntity>()

    for (simNode in similarNodesList) {
        val nodes = repository.getNeighborsOfNodeById(simNode.first.id)

        for (n in nodes) {
            if (simNode.first.id == n.id) continue
            val edge = repository.getEdgeBetweenNodes(simNode.first.id, n.id)
            neighborEdges.add(edge)
        }
        nodes.forEach { node ->
            if (neighborNodes.none { it.id == node.id }) {
                neighborNodes.add(node)
            }
        }
        if (neighborNodes.none { it.id == simNode.first.id }) {
            neighborNodes.add(simNode.first)
        }
    }

    // Build the response
    return Triple(
        neighborNodes.toList() to neighborEdges.toList(),
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

