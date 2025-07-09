package com.example.graphapp.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.graphapp.data.GraphRepository
import com.example.graphapp.data.VectorRepository
import com.example.graphapp.data.local.Event
import com.example.graphapp.data.api.ApiResponse
import com.example.graphapp.data.api.EventRecommendationResult
import com.example.graphapp.data.schema.GraphSchema.edgeLabels
import com.example.graphapp.data.schema.GraphSchema.keyNodes
import com.example.graphapp.data.api.ResponseData
import com.example.graphapp.data.local.EdgeEntity
import com.example.graphapp.data.local.NodeEntity
import com.example.graphapp.data.local.NodeWithoutEmbedding
import com.example.graphapp.data.schema.GraphSchema
import com.example.graphapp.data.schema.detectReplicateInput
import com.example.graphapp.data.schema.findPatterns
import com.example.graphapp.data.schema.initialiseSemanticSimilarityMatrix
import com.example.graphapp.data.schema.recommendOnInput
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

    // For ObjectBox Testing
    private val vectorRepository = VectorRepository(application)

    init {
        viewModelScope.launch(Dispatchers.IO) {

            vectorRepository.initializeEmbedding()
            vectorRepository.initialiseVectorRepository()
            val nodes = vectorRepository.getAllNodesWithoutEmbedding()
            val edges = vectorRepository.getAllEdges()
            val json = convertToJsonVector(nodes, edges)
            _graphData.value = json
        }
    }

    fun getNodeTypes(): List<String> {
        return GraphSchema.keyNodes + GraphSchema.propertyNodes + GraphSchema.otherNodes
    }

    private fun convertToJsonVector(nodes: List<NodeEntity>, edges: List<EdgeEntity>): String {
        val gson = Gson()
        val nodeList = nodes.map { mapOf("id" to it.name, "type" to it.type) }
        val edgeList = edges.map { edge ->
            val source = nodes.find { it.id == edge.fromId }?.name
            val target = nodes.find { it.id == edge.toId }?.name
            mapOf("source" to source, "target" to target, "label" to edge.edgeType)
        }
        val json = mapOf("nodes" to nodeList, "links" to edgeList)
        return gson.toJson(json)
    }

    private fun reloadGraphData() {
        val nodes = vectorRepository.getAllNodes()
        val edges = vectorRepository.getAllEdges()
        val json = convertToJsonVector(nodes, edges)
        _graphData.value = json
    }

    // Function 1: Predict missing properties
    fun fillMissingLinks() {

        // Creating updated graph
        val (newEdges, response) = predictMissingProperties(vectorRepository)
        val nodes = vectorRepository.getAllNodes()
        val edges = vectorRepository.getAllEdges() + newEdges
        val json = convertToJsonVector(nodes, edges)
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
    suspend fun provideEventRec( map: Map<String, String> ) {

        val normalizedMap = map.filterValues { it.isNotBlank() }
        if (normalizedMap.isEmpty()) { return }

        for ((type, value) in normalizedMap) {
            vectorRepository.insertNodeIntoDb(inputName = value, inputType = type)
        }
        for ((type1, value1) in normalizedMap) {
            for ((type2, value2) in normalizedMap) {
                if (type1 != type2) {
                    val edgeType = edgeLabels["$type1-$type2"]
                    if (edgeType != null) {
                        vectorRepository.insertEdgeIntoDB(
                            fromNode = NodeEntity(name = value1, type = type1),
                            toNode = NodeEntity(name = value2, type = type2),
                        )
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
        val simMatrix = initialiseSemanticSimilarityMatrix(vectorRepository)
        val response = detectReplicateInput(normalizedMap, vectorRepository)
        val apiRes = ApiResponse(
            status = "success",
            timestamp = "",
            data = ResponseData.DetectReplicaEventData(response)
        )
        Log.d("DetectReplicaEvent", "Output: $apiRes")

        // Create updated graph
        val noKeyTypes = normalizedMap.keys.none { it in keyNodes }
        val (nodes, edges, result) = recommendOnInput(normalizedMap, vectorRepository, noKeyTypes, simMatrix)
        val json = convertToJsonVector(nodes, edges)
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
        val response = findPatterns(vectorRepository)

        val apiRes = ApiResponse(
            status = "success",
            timestamp = "",
            data = ResponseData.PatternFindingData(response)
        )
        Log.d("GraphPatterns", "Response: $apiRes")

        return
    }
}
