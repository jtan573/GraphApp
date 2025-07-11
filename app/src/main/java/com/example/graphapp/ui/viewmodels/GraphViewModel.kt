package com.example.graphapp.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.graphapp.data.VectorRepository
import com.example.graphapp.data.local.Event
import com.example.graphapp.data.api.ApiResponse
import com.example.graphapp.data.api.EventRecommendationResult
import com.example.graphapp.data.api.ResponseData
import com.example.graphapp.data.local.EdgeEntity
import com.example.graphapp.data.local.NodeEntity
import com.example.graphapp.data.schema.GraphSchema.SchemaEdgeLabels
import com.example.graphapp.data.schema.GraphSchema.SchemaKeyNodes
import com.example.graphapp.data.schema.GraphSchema.SchemaOtherNodes
import com.example.graphapp.data.schema.GraphSchema.SchemaPropertyNodes
import com.example.graphapp.data.schema.detectReplicateInput
import com.example.graphapp.data.schema.findPatterns
import com.example.graphapp.data.schema.initialiseSemanticSimilarityMatrix
import com.example.graphapp.data.schema.predictMissingProperties
import com.example.graphapp.data.schema.recommendEventForEvent
import com.example.graphapp.data.schema.recommendEventsForProps
import com.example.graphapp.data.schema.updateSemanticSimilarityMatrix
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.map
import kotlin.text.isNotBlank

class GraphViewModel(application: Application) : AndroidViewModel(application) {

    private val _graphData = MutableStateFlow<String?>(null)
    val graphData: StateFlow<String?> = _graphData

    private val _createdEvents = MutableStateFlow<List<String>>(emptyList())
    val createdEvents: StateFlow<List<String>> = _createdEvents

    private val _filteredGraphData = MutableStateFlow<String?>(null)
    val filteredGraphData: StateFlow<String?> = _filteredGraphData

    private val vectorRepository = VectorRepository(application)

    // Similarity Matrix
    private var _simMatrix: Map<Pair<Long, Long>, Float>? = null
    val simMatrix: Map<Pair<Long, Long>, Float>
        get() = _simMatrix ?: initialiseSemanticSimilarityMatrix(vectorRepository).also {
            _simMatrix = it
        }

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
        return SchemaKeyNodes + SchemaPropertyNodes + SchemaOtherNodes
    }

    private fun convertToJsonVector(nodes: List<NodeEntity>, edges: List<EdgeEntity>): String {
        val gson = Gson()
        val nodeList = nodes.map { mapOf("id" to it.name, "type" to it.type) }
        val edgeList = edges.map { edge ->
            val source = nodes.find { it.id == edge.firstNodeId }?.name
            val target = nodes.find { it.id == edge.secondNodeId }?.name
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

    private fun reloadSimMatrix(
        repository: VectorRepository,
        simMatrix: MutableMap<Pair<Long, Long>, Float>,
        newEventMap: Map<String, String>,
    ) {
        _simMatrix = updateSemanticSimilarityMatrix(repository, simMatrix, newEventMap)
    }

    // Function 1: Predict missing properties
    fun fillMissingLinks() {

        // Creating updated graph
        val (newEdges, response) = predictMissingProperties(vectorRepository, simMatrix)
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
    fun provideEventRecOnInsert( map: Map<String, String> ) {

        val normalizedMap = map.filterValues { it.isNotBlank() }
        if (normalizedMap.isEmpty()) { return }

        // Check if its a replica event
        val isDuplicateEvent = detectDuplicateEvent(normalizedMap)
        if (isDuplicateEvent == true) return

        viewModelScope.launch {
            // If not replica event, then add into DB
            for ((type, value) in normalizedMap) {
                vectorRepository.insertNodeIntoDb(inputName = value, inputType = type)
            }
            for ((type1, value1) in normalizedMap) {
                for ((type2, value2) in normalizedMap) {
                    if (type1 != type2) {
                        val edgeType = SchemaEdgeLabels["$type1-$type2"]
                        if (edgeType != null) {
                            vectorRepository.insertEdgeIntoDB(
                                fromNode = vectorRepository.getNodeByNameAndType(value1, type1),
                                toNode = vectorRepository.getNodeByNameAndType(value2, type2)
                            )
                        }
                    }
                }
            }

            // Retrieve graph from db
            withContext(Dispatchers.Default) {
                reloadGraphData()

                val currentList = _createdEvents.value.toMutableList()
                currentList.add(Event(normalizedMap).toString())
                _createdEvents.value = currentList
            }

            // Update the similarity matrix
            reloadSimMatrix(
                vectorRepository, simMatrix.toMutableMap(), normalizedMap
            )

            // Create updated graph
            val noKeyTypes = normalizedMap.keys.none { it in SchemaKeyNodes }

            val (nodes, edges, result) = if (noKeyTypes) {
                recommendEventsForProps(normalizedMap, vectorRepository, simMatrix)
            } else {
                recommendEventForEvent(normalizedMap, vectorRepository, simMatrix)
            }

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
        }
        return
    }

    fun provideEventRecOnQuery(map: Map<String, String>) {

        val normalizedMap = map.filterValues { it.isNotBlank() }
        if (normalizedMap.isEmpty()) { return }

        // Check if its a replica event
        val isDuplicateEvent = detectDuplicateEvent(normalizedMap)
        if (isDuplicateEvent == true) return

        viewModelScope.launch {
            // Retrieve graph from db
            withContext(Dispatchers.Default) { }

            // Create updated graph
            val noKeyTypes = normalizedMap.keys.none { it in SchemaKeyNodes }

            val (nodes, edges, result) = if (noKeyTypes) {
                recommendEventsForProps(normalizedMap, vectorRepository, simMatrix)
            } else {
                recommendEventForEvent(normalizedMap, vectorRepository, simMatrix)
            }

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
        }
        return
    }

    // Function 4: Find Patterns/Clusters
    fun findGraphRelations() {
        val response = findPatterns(vectorRepository, simMatrix)

        val apiRes = ApiResponse(
            status = "success",
            timestamp = "",
            data = ResponseData.PatternFindingData(response)
        )
        Log.d("GraphPatterns", "Response: $apiRes")

        return
    }

    // Function 5: Detect Same Event
    fun detectDuplicateEvent(normalizedMap: Map<String, String>): Boolean {
        // Check if its a replica event
        val response = detectReplicateInput(normalizedMap, vectorRepository)
        val apiRes = ApiResponse(
            status = "success",
            timestamp = "",
            data = ResponseData.DetectReplicaEventData(response)
        )
        Log.d("DetectReplicaEvent", "Output: $apiRes")
        return response.isLikelyDuplicate
    }


}
