package com.example.graphapp.data.schema

import android.util.Log
import com.example.graphapp.data.GraphRepository
import com.example.graphdb.Edge
import com.example.graphdb.Node
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

private const val DECAY_FACTOR = 0.8f
private const val ITERATIONS = 5

fun graphCompletion(
    repository: GraphRepository
): Map<Long, List<Pair<String, Long>>> {

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

    val predictions = mutableMapOf<Long, List<Pair<String, Long>>>()

    val requiredProperties = GraphSchema.propertyNodes
    val allKeyNodes = allNodes.filter { it.type in GraphSchema.keyNodes }

    for (node in allKeyNodes) {

        val existingProperties = repository.getNeighborsOfNodeById(node.id).map { it.type }
        val missingProps = requiredProperties.filter { prop ->
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
                val simScore = sim.getOrDefault(node.id to candidate.id, 0f)

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
            predictions[node.id] = nodePredictions
            Log.d("Predictions", "Individual Predictions: $nodePredictions")
        }
    }

    Log.d("Predictions", "Predictions: $predictions")

    return predictions
}

fun createCompleteGraph(
    completions: Map<Long, List<Pair<String, Long>>>
) : List<Edge> {

    val newEdges = mutableListOf<Edge>()

    for ((id, predictions) in completions) {
        for ((prop, nodeId) in predictions) {
            val relationType = "Suggest"
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