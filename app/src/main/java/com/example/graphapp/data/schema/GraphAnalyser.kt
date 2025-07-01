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

fun graphCompletion(
    repository: GraphRepository
): List<Edge> {

    val simMatrix = initialiseSimilarityMatrix(repository)

    val completions = mutableMapOf<Long, List<Pair<String, Long>>>()

    val allNodes = repository.getAllNodes()
    val allKeyNodes = allNodes.filter { it.type in keyNodes }

    for (node in allKeyNodes) {

        val existingProperties = repository.getNeighborsOfNodeById(node.id).map { it.type }
        val missingProps = propertyNodes.filter { prop ->
            prop !in existingProperties
        }

        if (missingProps.isEmpty()) continue

        val nodePredictions = mutableListOf<Pair<String, Long>>()

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
                nodePredictions.add(prop to bestValue)
            }
        }

        if (nodePredictions.isNotEmpty()) {
            completions[node.id] = nodePredictions
            Log.d("Predictions", "Individual Predictions: $nodePredictions")
        }
    }

    Log.d("Predictions", "Predictions: $completions")

    val newEdges = mutableListOf<Edge>()

    for ((id, predictions) in completions) {
        for ((prop, nodeId) in predictions) {
            val relationType = "Suggest-$prop"
            val newEdge = Edge(
                id = -1L,
                fromNode = nodeId,
                toNode = id,
                relationType = relationType
            )
            newEdges.add(newEdge)
        }
    }
    return newEdges
}

fun findExistingRelations(
    repository: GraphRepository
) : List<Edge> {
    val simMatrix = initialiseSimilarityMatrix(repository)

    // Collect all candidate key nodes, grouped by type
    val allNodes = repository.getAllNodes()
    val relevantNodes = allNodes
        .filter { it.type in (keyNodes) }
//        .groupBy { it.type }

    val allScores = mutableListOf<Triple<Long, Long, Float>>()

    for (node in relevantNodes) {
        for (other in relevantNodes) {
            if ( node.id == other.id ) continue

            val (small, large) = if (node.id < other.id) node.id to other.id else other.id to node.id
            val s = simMatrix.getOrDefault(small to large, 0f)

            if (s>=0.20f) continue // skip zero similarity
            allScores.add(Triple(small, large, s))
        }
    }

    val topScoringRelations = allScores.toSet().toList()
        .sortedByDescending { it.third }
        .take(5)

    Log.d("topScoringRelations", "Here: $topScoringRelations")

    val floats = allScores.map { it.third }
    val sorted = floats.sorted()
    fun percentile(p: Double): Float {
        if (sorted.isEmpty()) return 0f
        val index = ((p / 100.0) * (sorted.size - 1)).toInt()
        return sorted[index]
    }

    Log.d("Distribution", "25th percentile: ${percentile(25.0)}")
    Log.d("Distribution", "50th percentile: ${percentile(50.0)}")
    Log.d("Distribution", "75th percentile: ${percentile(75.0)}")

    val newEdges = mutableListOf<Edge>()

    for ((id1, id2, _) in topScoringRelations) {
        val relationType = "Related"
        val newEdge = Edge(
            id = -1L,
            fromNode = id1,
            toNode = id2,
            relationType = relationType
        )
        newEdges.add(newEdge)
    }
    return newEdges
}

fun respondIncomingEvent(
    newEventMap: Map<String, String>,
    repository: GraphRepository
) : Pair<List<Node>, List<Edge>> {

    val simMatrix = initialiseSimilarityMatrix(repository)

    val allNodes = repository.getAllNodes()
    val allNodesByType = allNodes
        .filter { it.type in (keyNodes + propertyNodes) }
        .groupBy { it.type }

    // Compute similarity of each candidate to all event nodes
    val eventNodeIds = newEventMap.entries.map { (type, value) ->
        repository.findNodeByNameAndType(value, type)
    }.toSet()

    // For each key node type, compute top 3
    val topRecommendationsByType = mutableMapOf<String, List<Pair<Long, Float>>>()

    for ((type, candidates) in allNodesByType) {
        val scoresForType = mutableListOf<Pair<Long, Float>>()

        for (candidate in candidates) {
            if (candidate.id in eventNodeIds) continue // skip the event key nodes themselves
            val avg = eventNodeIds.map { eventId ->
                val s = simMatrix.getOrDefault(candidate.id to eventId, 0f)
                s
            }.average().toFloat()
            scoresForType.add(candidate.id to avg)
        }

        val topForType = scoresForType
            .sortedByDescending { it.second }
            .take(2)

        topRecommendationsByType[type] = topForType
    }

    val neighborNodes = mutableListOf<Node>()
    val neighborEdges = mutableListOf<Edge>()

    val eventKeyIds = newEventMap
        .filter { (type, value) -> value.isNotBlank() && type in (keyNodes + propertyNodes) }
        .mapNotNull { (type, value) ->
            repository.findNodeByNameAndType(value, type)
        }
    val allPredictedNodes = topRecommendationsByType.values.flatten().map { it.first }

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
        for ((type, nodeList) in topRecommendationsByType) {
            for ((predictedId, _) in nodeList) {
                val relationType = "Suggest-$type"
                val newEdge = Edge(
                    id = -1L,
                    fromNode = id,
                    toNode = predictedId,
                    relationType = relationType
                )
                neighborEdges.add(newEdge)
                Log.d(
                    "New Edges",
                    "Type=$type Predicted: $newEdge"
                )
            }
        }
    }

    val completions = mutableMapOf<Long, List<Pair<String, Long>>>()

    for (nodeId in eventKeyIds) {

        val node = repository.findNodeById(nodeId)
        val existingProperties = repository.getNeighborsOfNodeById(nodeId).map { it.type }
        val missingProps = propertyNodes.filter { prop ->
            prop !in existingProperties
        }

        if (missingProps.isEmpty()) continue

        val nodePredictions = mutableListOf<Pair<String, Long>>()

        // Iterate over the missing properties
        for (prop in missingProps) {

            // Collect candidates of same node type with this property
            val candidates = allNodes.filter { candidate ->
                candidate.id != node?.id && candidate.type == node?.type && repository
                    .getNeighborsOfNodeById(candidate.id)
                    .any { neighbour -> neighbour.type == prop }
            }

            val scoreByValue = mutableMapOf<Long, Float>()

            for (candidate in candidates) {
                val simScore = simMatrix.getOrDefault(node?.id to candidate.id, 0f)

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
                nodePredictions.add(prop to bestValue)
            }
        }
    }

    Log.d("Predictions", "Predictions: $completions")

    for ((id, predictions) in completions) {
        for ((prop, nodeId) in predictions) {
            val relationType = "Suggest-$prop"
            val newEdge = Edge(
                id = -1L,
                fromNode = nodeId,
                toNode = id,
                relationType = relationType
            )
            neighborEdges.add(newEdge)
        }
    }

    return neighborNodes to neighborEdges
}