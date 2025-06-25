package com.example.graphapp.ui.viewmodels

import android.app.Application
import android.media.metrics.Event
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.graphapp.data.GraphRepository
import com.example.graphapp.data.classes.EventInput
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

    fun getEdgeTypes(): List<String> {
        return repository.getEdgeTypes()
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
                        repository.insertEdge(fromNode = value1, toNode = value2, relationType = edgeType)
                    }
                }
            }
        }
        reloadGraphData()
        Log.d("createEvent", "Event creation complete.")
    }
}