package com.example.graphapp.data.schema

import android.util.Log
import com.example.graphapp.data.GraphRepository
import com.example.graphapp.data.schema.GraphSchema.propertyNodes
import com.example.graphapp.data.schema.GraphSchema.keyNodes
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

interface GraphScoringStrategy {
    fun score(
        newEventMap: Map<String, String>,
        repository: GraphRepository
    ): List<Long>
}

object CommonNeighbourScoring : GraphScoringStrategy {
    override fun score(
        newEventMap: Map<String, String>,
        repository: GraphRepository
    ): List<Long> {

        // Finding key nodes around specified property nodes
        val existingLinks = mutableMapOf<String, MutableSet<Long>>()
        for ((type, value) in newEventMap) {
            if (type in propertyNodes) {
                val propertyNodeId = repository.findNodeByNameAndType(value, type)
                // Getting key nodes based on property nodes
                val keyNodeIds = repository.findFromNodeByToNode(propertyNodeId)
                // property node / type -> key nodes
                existingLinks.getOrPut(type) { mutableSetOf() }.addAll(keyNodeIds)
            }
        }

        // Getting most common key node
        val frequencyMap = mutableMapOf<Long, Int>()
        for (nodeSet in existingLinks.values) {
            for (nodeId in nodeSet) {
                frequencyMap[nodeId] = frequencyMap.getOrDefault(nodeId, 0) + 1
            }
        }
        val topPredictions = frequencyMap.entries
            .sortedByDescending { it.value }.take(3).map { it.key }

        return topPredictions
    }
}

object SimRankScoring : GraphScoringStrategy {

    private const val DECAY_FACTOR = 0.8f
    private const val ITERATIONS = 5

    override fun score(newEventMap: Map<String, String>, repository: GraphRepository): List<Long> {

        val allNodes = repository.getAllNodes()
        val nodeIds = allNodes.map { it.id }

        val edges = repository.getAllEdges()
        val neighborMap = mutableMapOf<Long, MutableSet<Long>>()
        for (edge in edges) {
            neighborMap.getOrPut(edge.fromNode) { mutableSetOf() }.add(edge.toNode)
            neighborMap.getOrPut(edge.toNode) { mutableSetOf() }.add(edge.fromNode)
        }

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

        // 5. Collect event **key nodes** only
        val eventKeyNodeIds = newEventMap.entries.mapNotNull { (type, value) ->
            if (type in keyNodes) {
                repository.findNodeByNameAndType(value, type)
            } else {
                null
            }
        }.toSet()

        // 6. Collect **all key nodes** in graph
        val allKeyNodeIds = allNodes.filter { it.type in keyNodes }
            .map { it.id }
            .toSet()

        // 7. Compute average similarity of each candidate key node to event key nodes
        val scores = mutableMapOf<Long, Float>()
        for (candidate in allKeyNodeIds) {
            if (candidate in eventKeyNodeIds) continue // skip the event key nodes themselves
            val avg = eventKeyNodeIds.map { eventId ->
                val s = sim.getOrDefault(candidate to eventId, 0f)
                Log.d("SCORES_DETAIL", "candidate=$candidate eventId=$eventId sim=$s")
                s
            }.average().toFloat()
            scores[candidate] = avg
        }

        // For debugging
        val topScores = scores.entries.sortedByDescending { it.value }.take(3)
        Log.d("SCORES","Scores: $topScores")

        // 8. Return top 3 most similar key nodes
        return scores.entries.sortedByDescending { it.value }.take(3).map { it.key }
    }
}
