package com.example.graphapp.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.ui.input.key.Key
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.graphapp.data.repository.VectorRepository
import com.example.graphapp.data.schema.Event
import com.example.graphapp.data.api.ApiResponse
import com.example.graphapp.data.api.EventRecommendationResult
import com.example.graphapp.data.api.ResponseData
import com.example.graphapp.data.local.EdgeEntity
import com.example.graphapp.data.local.NodeEntity
import com.example.graphapp.data.schema.UiEvent
import com.example.graphapp.data.schema.GraphSchema.SchemaKeyNodes
import com.example.graphapp.data.schema.GraphSchema.SchemaOtherNodes
import com.example.graphapp.data.schema.GraphSchema.SchemaPropertyNodes
import com.example.graphapp.data.local.computeSemanticMatrixForQuery
import com.example.graphapp.data.local.detectReplicateInput
import com.example.graphapp.data.local.findPatterns
import com.example.graphapp.data.local.initialiseSemanticSimilarityMatrix
import com.example.graphapp.data.local.predictMissingProperties
import com.example.graphapp.data.local.recommendEventForEvent
import com.example.graphapp.data.local.recommendEventsForProps
import com.example.graphapp.data.local.updateSemanticSimilarityMatrix
import com.example.graphapp.domain.usecases.findRelevantContactsUseCase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var _simMatrix: Map<Pair<Long, Long>, Float>? = null
    val simMatrix: Map<Pair<Long, Long>, Float>
        get() = _simMatrix ?: initialiseSemanticSimilarityMatrix(vectorRepository).also {
            _simMatrix = it
        }

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

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

    private fun createFullGraph(nodes: List<NodeEntity>, edges: List<EdgeEntity>) {
        val json = convertToJsonVector(nodes, edges)
        _graphData.value = json
    }

    private fun createFilteredGraph(nodes: List<NodeEntity>, edges: List<EdgeEntity>) {
        val json = convertToJsonVector(nodes, edges)
        _filteredGraphData.value = json
    }

    // Function 1: Predict missing properties
    fun fillMissingLinks() {

        // Creating updated graph
        val (newEdges, response) = predictMissingProperties(vectorRepository, simMatrix)
        val nodes = vectorRepository.getAllNodes()
        val edges = vectorRepository.getAllEdges() + newEdges
        createFullGraph(nodes, edges)

        // Creating API response
        val apiRes = ApiResponse(
            status = "success",
            timestamp = "",
            data = ResponseData.PredictMissingPropertiesData(response)
        )
        Log.d("PredictMissingLinks", "Response: $apiRes")

        return
    }

    // Function 2/3/5: Predict Top Relationships based on Incoming Event/Detect input anomaly
    fun provideEventRecommendation(map: Map<String, String>, isQuery: Boolean, queryKey: String? = null) {

        // Logic starts here
        viewModelScope.launch {

            val normalizedMap = map.filterValues { it.isNotBlank() }
            if (normalizedMap.isEmpty()) {
                return@launch
            }

            val noKeyTypes = normalizedMap.keys.none { it in SchemaKeyNodes }

            // For entries with no key nodes
            if (noKeyTypes) {
                val (nodes, edges, result) = recommendEventsForProps(normalizedMap, vectorRepository, queryKey)
                createFilteredGraph(nodes, edges)

                if (result is EventRecommendationResult.PropertyToEventRec) {
                    val apiRes = ApiResponse(
                        status = "success",
                        timestamp = "",
                        data = ResponseData.DiscoverEventsData(result.items)
                    )
                    Log.d("RecommendRelatedEvents", "Response: $apiRes")
                }
                return@launch
            }

            // Check if its a duplicate event
            val (isDuplicateEvent, duplicateNode) = detectDuplicateEvent(normalizedMap)

            // Update created event list for UI
            val currentList = _createdEvents.value.toMutableList()
            currentList.add(Event(normalizedMap).toString())
            _createdEvents.value = currentList

            // Action based on type of input
            val (newEventNodes, filteredSimMatrix) = prepareNewEventNodesAndMatrix(
                isDuplicateEvent = isDuplicateEvent,
                duplicateNode = duplicateNode,
                isQuery = isQuery,
                normalizedMap = normalizedMap,
                vectorRepository = vectorRepository,
                simMatrix = simMatrix,
                reloadGraphData = { reloadGraphData() },
                reloadSimMatrix = { repo, matrix, map -> reloadSimMatrix(repo, matrix, map) }
            )

            // Get recommendations and results
            val (nodes, edges, result) =
                if (isQuery && !isDuplicateEvent) {
                    recommendEventForEvent(normalizedMap, vectorRepository, filteredSimMatrix, newEventNodes, queryKey, true)
                } else {
                    recommendEventForEvent(normalizedMap, vectorRepository, simMatrix, newEventNodes, queryKey, false)
                }

            // Create update graph
            createFilteredGraph(nodes, edges)

            // Create API response
            if (result is EventRecommendationResult.EventToEventRec) {
                val apiRes = ApiResponse(
                    status = "success",
                    timestamp = "",
                    data = ResponseData.ProvideRecommendationsData(result.items)
                )
                Log.d("RecommendRelatedEvents", "Response: $apiRes")
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
    suspend fun detectDuplicateEvent(normalizedMap: Map<String, String>):
            Pair<Boolean, NodeEntity?> {

        val (duplicateNode, response) = detectReplicateInput(normalizedMap, vectorRepository)
        val apiRes = ApiResponse(
            status = "success",
            timestamp = "",
            data = ResponseData.DetectReplicaEventData(response)
        )
        Log.d("DetectReplicaEvent", "Output: $apiRes")

        if (response.isLikelyDuplicate == true) {
            _uiEvent.trySend(UiEvent.ShowSnackbar("Very similar event(s) found."))
        }

        return response.isLikelyDuplicate to duplicateNode
    }

    // Use Case Function
    suspend fun findRelevantContacts(map: Map<String, String>) {
        val normalizedMap = map.filterValues { it.isNotBlank() }
        if (normalizedMap.isEmpty()) return

        val (nodes, edges, result) = findRelevantContactsUseCase(
            map, vectorRepository, simMatrix
        )

        createFilteredGraph(nodes, edges)
    }
}
