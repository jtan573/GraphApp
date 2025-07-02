package com.example.graphapp.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.graphapp.data.GraphRepository
import com.example.graphapp.data.local.Event
import com.example.graphapp.data.schema.GraphSchema.edgeLabels
import com.example.graphapp.data.schema.PredictMissingPropertiesResponse
import com.example.graphapp.data.schema.ProvideRecommendationsResponse
import com.example.graphapp.data.schema.eventToEventRecommendation
import com.example.graphapp.data.schema.findPatterns
import com.example.graphapp.data.schema.predictMissingProperties
import com.example.graphdb.Edge
import com.example.graphdb.Node
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.map
import kotlin.text.isNotBlank

class GraphViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GraphRepository(application)

    private val _graphData = MutableStateFlow<String?>(null)
    val graphData: StateFlow<String?> = _graphData

    private val _createdEvents = MutableStateFlow<List<String>>(emptyList())
    val createdEvents: StateFlow<List<String>> = _createdEvents

    private val _filteredGraphData = MutableStateFlow<String?>(null)
    val filteredGraphData: StateFlow<String?> = _filteredGraphData

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

    // Function 1: Predict missing properties
    fun fillMissingLinks() {

        // Creating updated graph
        val (newEdges, predictions) = predictMissingProperties(repository)
        val nodes = repository.getAllNodes()
        val edges = repository.getAllEdges() + newEdges
        val json = convertToJson(nodes, edges)
        _graphData.value = json

        // Creating API response
        val response = PredictMissingPropertiesResponse(predictions)
        Log.d("PredictMissingLinks", "Predict Response: $response")

        // Test
        findPatterns(repository)

        return
    }

    // Function 2: Relate existing events (Past data)
//    fun findGraphRelations() {
//        val newEdges = findExistingRelations(repository)
//        val nodes = repository.getAllNodes()
//        val edges = repository.getAllEdges() + newEdges
//        val json = convertToJson(nodes, edges)
//        _graphData.value = json
//        return
//    }

    // Function 2/3: Predict Top Relationships based on Incoming Event
    fun provideEventRec( map: Map<String, String> ) {

        val normalizedMap = map.filterValues { it.isNotBlank() }
        if (normalizedMap.isEmpty()) { return }

        for ((type, value) in normalizedMap) {
            repository.insertNode(value, type)
        }
        for ((type1, value1) in normalizedMap) {
            for ((type2, value2) in normalizedMap) {
                if (type1 != type2) {
                    val edgeType = edgeLabels["$type1-$type2"]
                    if (edgeType != null) {
                        repository.insertEdge(fromNodeName = value1, fromNodeType = type1,
                            toNodeName = value2, toNodeType = type2, relationType = edgeType)
                    }
                }
            }
        }

        // Retrieve graph from db
        reloadGraphData()

        // Add to event logs
        val currentList = _createdEvents.value.toMutableList()
        currentList.add(Event(normalizedMap).toString())
        _createdEvents.value = currentList

        // Create updated graph
        val (nodes, edges, recs) = eventToEventRecommendation(normalizedMap, repository)
        val json = convertToJson(nodes, edges)
        _filteredGraphData.value = json

        // Create API response
        val response = ProvideRecommendationsResponse(normalizedMap, recs)
        Log.d("Recommend Related Events", "Recommendations: $response")

        return
    }


}
