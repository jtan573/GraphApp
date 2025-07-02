package com.example.graphapp.data.schema

import android.util.Log
import com.example.graphapp.data.GraphRepository
import com.example.graphapp.data.schema.GraphSchema.keyNodes
import com.example.graphapp.data.schema.GraphSchema.propertyNodes
import com.example.graphdb.Edge
import com.example.graphdb.Node
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

private const val DECAY_FACTOR = 0.8f
private const val ITERATIONS = 5

fun initialiseSimilarityMatrix(
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

/**
 * Predicts missing properties for nodes in the graph and generates suggested edges to visualize the predictions.
 *
 * @param repository The [GraphRepository] providing access to nodes and edges in the graph.
 * @return A [Pair] containing:
 *   - A [List] of [Edge] objects representing suggested property relations for visualization.
 *   - A [List] of [PredictMissingProperties] containing detailed prediction results per node.
 */
fun predictMissingProperties(
    repository: GraphRepository
): Pair<List<Edge>, List<PredictMissingProperties>> {

    val simMatrix = initialiseSimilarityMatrix(repository)

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
        val predictedPropertiesMap = mutableMapOf<String, String>()

        // Iterate over the missing properties
        for (prop in missingProps) {

            // Collect candidates of same node type with this property
            val candidates = allNodes.filter { candidate ->
                candidate.id != node.id && candidate.type == node.type && repository
                    .getNeighborsOfNodeById(candidate.id)
                    .any { neighbour -> neighbour.type == prop }
            }

            val scoreByValue = mutableMapOf<Long, Float>()

            for (candidate in candidates) {
                val simScore = simMatrix.getOrDefault(node.id to candidate.id, 0f)

                // Retrieve the property value of this candidate
                val propValue = repository
                    .getNeighborsOfNodeById(candidate.id)
                    .firstNotNullOf { neighbour ->
                        if (neighbour.type == prop) { neighbour.id } else null
                    }

                scoreByValue[propValue] = scoreByValue.getOrDefault(propValue, 0f) + simScore
            }

            if (scoreByValue.isNotEmpty()) {
                val (bestValue, _) = scoreByValue.maxByOrNull { it.value }!!
                predictedPropertiesMap[prop] = repository.findNodeById(bestValue)!!.name
                flag = true
            }
        }

        if (flag) {

            // Build PredictMissingProperties object
            // Prepare existingProperties Map<String, String> for NodeDetails
            val existingPropertiesMap = existingPropertiesNodes.associate { n ->
                n.type to n.name
            }

            val predictMissing = PredictMissingProperties(
                nodeId = node.id,
                nodeDetails = NodeDetails(
                    type = node.type,
                    name = node.name,
                    existingProperties = existingPropertiesMap
                ),
                predictedProperties = predictedPropertiesMap
            )
            predictionsList.add(predictMissing)

            Log.d("Predictions", "Individual Predictions: $predictedPropertiesMap")
        }
    }

    // Creating Edges for UI
    val newEdges = mutableListOf<Edge>()

    for (prediction in predictionsList) {
        val targetNodeId = prediction.nodeId
        for ((prop, sourceNodeIdString) in prediction.predictedProperties) {
            val relationType = "Suggest-$prop"
            val newEdge = Edge(
                id = -1L,
                fromNode = repository.findNodeByNameAndType(sourceNodeIdString, prop),
                toNode = targetNodeId,
                relationType = relationType
            )
            newEdges.add(newEdge)
        }
    }

    return newEdges to predictionsList
}

// Function 2: Provide event recommendations on input event
fun eventToEventRecommendation(
    newEventMap: Map<String, String>,
    repository: GraphRepository
) : Triple<List<Node>, List<Edge>, List<Recommendation>> {

    val simMatrix = initialiseSimilarityMatrix(repository)

    val allNodes = repository.getAllNodes()
    val allNodesByType = allNodes
        .filter { it.type in (keyNodes + propertyNodes) } // Exclude description nodes
        .groupBy { it.type }

    // Compute similarity of each candidate to all event nodes
    val eventNodeIds = newEventMap.entries.map { (type, value) ->
        repository.findNodeByNameAndType(value, type)
    }.toSet()

    // For each key node type, compute top 2
    val topRecommendationsByType = mutableMapOf<String, List<Pair<Node, Float>>>()
    val recList = mutableListOf<Recommendation>()

    for ((type, candidates) in allNodesByType) {
        val scoresForType = mutableListOf<Pair<Node, Float>>()

        for (candidate in candidates) {
            if (candidate.id in eventNodeIds) continue // skip the event key nodes themselves
            val avg = eventNodeIds.map { eventId ->
                val s = simMatrix.getOrDefault(candidate.id to eventId, 0f)
                s
            }.average().toFloat()
            scoresForType.add(candidate to avg)
        }

        val topForType = scoresForType
            .sortedByDescending { it.second }
            .take(2)

        topRecommendationsByType[type] = topForType
    }

    for ((type, recs) in topRecommendationsByType) {
        val recsByTypeList = mutableListOf<String>()
        for (rec in recs) {
            recsByTypeList.add(rec.first.name)
        }
        recList.add(
            Recommendation(
                recType = type,
                recItems = recsByTypeList
            )
        )
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

    return Triple(neighborNodes, neighborEdges, recList)
}

// Function 3: Discover events
fun propToEventDiscovery(
    newEventMap: Map<String, String>,
    repository: GraphRepository
) : Triple<List<Node>, List<Edge>, List<PredictedEventByType>> {

    val simMatrix = initialiseSimilarityMatrix(repository)

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
//            if (candidate.id in eventNodeIds) continue // skip the event key nodes themselves
            val avg = eventNodeIds.map { eventId ->
                val s = simMatrix.getOrDefault(candidate.id to eventId, 0f)
                s
            }.average().toFloat()
            scoresForType.add(candidate to avg)
        }

        val topForType = scoresForType
            .sortedByDescending { it.second }
            .take(3)

        topRecommendationsByType[type] = topForType
    }

    // Creating API response
    val eventsByType = mutableListOf<PredictedEventByType>()

    for ((type, recs) in topRecommendationsByType) {
        val predictedEventsList = mutableListOf<EventDetails>()
        for (rec in recs) {
            val neighbourProps = repository.getNeighborsOfNodeById(rec.first.id)
                .filter { it.type in propertyNodes }
                .associate { it.type to it.name }
            predictedEventsList.add(
                EventDetails(
                    eventName = rec.first.name,
                    eventProperties = neighbourProps
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

    return Triple(neighborNodes, neighborEdges, eventsByType)
}

// Function 4: Find Patterns
fun findPatterns(
    repository: GraphRepository
) {
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
                 nodeProperties = targetNodeProps
             )
             keyNodesList.add(keyNode)
        }
        patternFindingResponse.add(keyNodesList)

        println("Pattern #$i: Nodes=$keyNodesList")
    }
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
