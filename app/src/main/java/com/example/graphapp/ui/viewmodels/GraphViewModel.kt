package com.example.graphapp.ui.viewmodels

import android.app.Application
import android.media.metrics.Event
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.graphapp.data.GraphRepository
import com.example.graphapp.data.classes.GraphAnalyser
import com.example.graphapp.data.classes.GraphSchema
import com.example.graphdb.Edge
import com.example.graphdb.Node
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.text.isNotBlank

class GraphViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GraphRepository(application)
    private val _graphData = MutableStateFlow<String?>(null)
    val graphData: StateFlow<String?> = _graphData
    val relationshipRules = GraphSchema.relationshipRules

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.initialiseDatabase()
            val nodes = repository.getAllNodes()
            val edges = repository.getAllEdges()
            val json = convertToJson(nodes, edges)
            _graphData.value = json
        }
    }

    fun getNodeTypes(): List<String> {
        return repository.getNodeTypes()
    }

    private fun convertToJson(nodes: List<Node>, edges: List<Edge>): String {
        val gson = Gson()
        val nodeList = nodes.map { mapOf("id" to it.name, "type" to it.type) }
        val edgeList = edges.map { edge ->
            val source = nodes.find { it.id == edge.fromNode }?.name
            val target = nodes.find { it.id == edge.toNode }?.name
            mapOf("source" to source, "target" to target, "label" to edge.relationType)
        }
        val json = mapOf("nodes" to nodeList, "links" to edgeList)
        return gson.toJson(json)
    }

    private fun reloadGraphData() {
        val nodes = repository.getAllNodes()
        val edges = repository.getAllEdges()
        val json = convertToJson(nodes, edges)
        _graphData.value = json
    }

    fun createEvent(map: Map<String, String>) {
        val normalizedMap = map.filterValues { it.isNotBlank() }
        for ((type, value) in normalizedMap) {
            repository.insertNode(value, type)
        }

        for ((type1, value1) in normalizedMap) {
            for ((type2, value2) in normalizedMap) {
                if (type1 != type2) {
                    val edgeType = relationshipRules["$type1-$type2"]
                    if (edgeType != null) {
                        repository.insertEdge(fromNodeName = value1, fromNodeType = type1,
                            toNodeName = value2, toNodeType = type2, relationType = edgeType)
                    }
                }
            }
        }
        reloadGraphData()
        predictTopLinks()
    }

    fun predictTopLinks(): List<Triple<Long, Long, Int>> {
        val nodes = repository.getAllNodes()
        val edges = repository.getAllEdges()
        val existingLinks: Set<Pair<Long, Long>> = edges.map { it.fromNode to it.toNode }.toSet()

        // Build neighbor map
        val neighborMap = mutableMapOf<Long, MutableSet<Long>>()
        for ((from, to, _) in edges) {
            neighborMap.getOrPut(from) { mutableSetOf() }.add(to)
            neighborMap.getOrPut(to) { mutableSetOf() }.add(from)
        }

        val predictions = mutableListOf<Triple<Long, Long, Int>>()

        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val a = nodes[i].id
                val b = nodes[j].id
                if ((a to b) !in existingLinks && (b to a) !in existingLinks) {
                    val score = GraphAnalyser.commonNeighborsScore(a, b, neighborMap)
                    if (score > 0.0) {
                        predictions.add(Triple(a, b, score))
                    }
                }
            }
        }

        val finalPredictions: List<Triple<Long, Long, Int>> =
            predictions.sortedByDescending { it.third }.take(2)

        Log.d("Predictions", "Final Predictions: $finalPredictions")

        return predictions.sortedByDescending { it.third }.take(2) // top 10 predicted links
    }
}