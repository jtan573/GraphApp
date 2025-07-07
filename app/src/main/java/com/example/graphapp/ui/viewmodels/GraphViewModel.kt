package com.example.graphapp.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.graphapp.data.GraphRepository
import com.example.graphapp.data.ObjectBoxRepository
import com.example.graphapp.data.local.Event
import com.example.graphapp.data.api.ApiResponse
import com.example.graphapp.data.api.DiscoverEventsResponse
import com.example.graphapp.data.api.EventRecommendationResult
import com.example.graphapp.data.api.PatternFindingResponse
import com.example.graphapp.data.schema.GraphSchema.edgeLabels
import com.example.graphapp.data.schema.GraphSchema.keyNodes
import com.example.graphapp.data.api.PredictMissingPropertiesResponse
import com.example.graphapp.data.api.ProvideRecommendationsResponse
import com.example.graphapp.data.api.ResponseData
import com.example.graphapp.data.local.EdgeEntity
import com.example.graphapp.data.local.NodeEntity
import com.example.graphapp.data.local.ObjectBox
import com.example.graphapp.data.schema.detectInputAnomaly
import com.example.graphapp.data.schema.findPatterns
import com.example.graphapp.data.schema.recommendOnInput
import com.example.graphapp.data.schema.predictMissingProperties
import com.example.graphdb.Edge
import com.example.graphdb.Node
import com.google.gson.Gson
import io.objectbox.kotlin.boxFor
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

    // For ObjectBox Testing
    private val obRepository = ObjectBoxRepository(application)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.initialiseDatabase()
            val nodes = repository.getAllNodes()
            val edges = repository.getAllEdges()
            val json = convertToJson(nodes, edges)
            _graphData.value = json

            obRepository.initializeEmbedding()
            testGraphEmbeddings()
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
        val (newEdges, response) = predictMissingProperties(repository)
        val nodes = repository.getAllNodes()
        val edges = repository.getAllEdges() + newEdges
        val json = convertToJson(nodes, edges)
        _graphData.value = json

        // Creating API response
        val apiRes = ApiResponse(
            status = "success",
            timestamp = "",
            data = ResponseData.PredictMissingPropertiesData(response)
        )

        Log.d("PredictMissingLinks", "Predict Response: $apiRes")

        return
    }

    // Function 2/3/5: Predict Top Relationships based on Incoming Event/Detect input anomaly
    fun provideEventRec( map: Map<String, String> ) {

        val normalizedMap = map.filterValues { it.isNotBlank() }
        if (normalizedMap.isEmpty()) { return }

        for ((type, value) in normalizedMap) {
            repository.insertNode(value, type, null)
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

        // Detect anomaly
        val response = detectInputAnomaly(normalizedMap, repository)
        val apiRes = ApiResponse(
            status = "success",
            timestamp = "",
            data = ResponseData.DetectAnomalyData(response)
        )
        Log.d("DetectInputAnomaly", "Anomaly: $apiRes")

        // Create updated graph
        val noKeyTypes = normalizedMap.keys.none { it in keyNodes }
        val (nodes, edges, result) = recommendOnInput(normalizedMap, repository, noKeyTypes)
        val json = convertToJson(nodes, edges)
        _filteredGraphData.value = json

        // Create API response
        when (result) {
            is EventRecommendationResult.EventToEventRec -> {
                val apiRes = ApiResponse(
                    status = "success",
                    timestamp = "",
                    data = ResponseData.ProvideRecommendationsData(result.items)
                )
                Log.d("RecommendRelatedEvents", "Response: $apiRes")

            }
            is EventRecommendationResult.PropertyToEventRec -> {
                val apiRes = ApiResponse(
                    status = "success",
                    timestamp = "",
                    data = ResponseData.DiscoverEventsData(result.items)
                )
                Log.d("RecommendRelatedEvents", "Response: $apiRes")
            }
        }
        return
    }


//     Function 4: Find Patterns/Clusters
    fun findGraphRelations() {
        val response = findPatterns(repository)

        val apiRes = ApiResponse(
            status = "success",
            timestamp = "",
            data = ResponseData.PatternFindingData(response)
        )
        Log.d("GraphPatterns", "Response: $apiRes")

        return
    }

    suspend fun testGraphEmbeddings() {
        val e1: FloatArray = obRepository.embedText("Delhi has a population 32 million")
        val e2: FloatArray = obRepository.embedText("What is the population of Delhi?")
        val e3: FloatArray =
            obRepository.embedText("Cities with a population greater than 4 million are termed as metro cities")

        val d12 = obRepository.cosineDistance(e1, e2)
        val d13 = obRepository.cosineDistance(e1, e3)
        println("Similarity between e1 and e2: $d12")
        println("Similarity between e1 and e3: $d13")
    }
}
