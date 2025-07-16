package com.example.graphapp.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.schema.Event
import com.example.graphapp.data.api.ApiResponse
import com.example.graphapp.data.api.ResponseData
import com.example.graphapp.data.api.buildApiResponseFromResult
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.embedding.SentenceEmbedding
import com.example.graphapp.data.schema.UiEvent
import com.example.graphapp.data.schema.GraphSchema.SchemaKeyNodes
import com.example.graphapp.data.schema.GraphSchema.SchemaOtherNodes
import com.example.graphapp.data.schema.GraphSchema.SchemaPropertyNodes
import com.example.graphapp.data.local.detectReplicateInput
import com.example.graphapp.data.local.findPatterns
import com.example.graphapp.data.local.initialiseSemanticSimilarityMatrix
import com.example.graphapp.data.local.predictMissingProperties
import com.example.graphapp.data.local.recommendEventForEvent
import com.example.graphapp.data.local.recommendEventsForProps
import com.example.graphapp.data.local.updateSemanticSimilarityMatrix
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.UserActionRepository
import com.example.graphapp.domain.usecases.findRelevantIncidentsUseCase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.collections.map
import kotlin.text.isNotBlank

class GraphViewModel(application: Application) : AndroidViewModel(application) {

    private val sentenceEmbedding = SentenceEmbedding()

    private val embeddingRepository = EmbeddingRepository(application)
    private val eventRepository = EventRepository(sentenceEmbedding)
    private val personnelRepository = UserActionRepository(sentenceEmbedding)

    private val _graphData = MutableStateFlow<String?>(null)
    val graphData: StateFlow<String?> = _graphData

    private val _filteredGraphData = MutableStateFlow<String?>(null)
    val filteredGraphData: StateFlow<String?> = _filteredGraphData

    private val _personnelData = MutableStateFlow<String?>(null)
    val personnelData: StateFlow<String?> = _personnelData

    private var _simMatrix: Map<Pair<Long, Long>, Float>? = null
    val simMatrix: Map<Pair<Long, Long>, Float>
        get() = _simMatrix ?: initialiseSemanticSimilarityMatrix(eventRepository).also {
            _simMatrix = it
        }

    private val _createdEvent = MutableStateFlow<String>("")
    val createdEvent: StateFlow<String> = _createdEvent

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {

            embeddingRepository.initializeEmbedding()
            eventRepository.initialiseEventRepository()
            val nodes = eventRepository.getAllEventNodesWithoutEmbedding()
            val edges = eventRepository.getAllEventEdges()
            val json = convertToJsonVector(nodes, edges)
            _graphData.value = json
        }
    }

    fun getNodeTypes(): List<String> {
        return SchemaKeyNodes + SchemaPropertyNodes + SchemaOtherNodes
    }

    private fun convertToJsonVector(nodes: List<EventNodeEntity>, edges: List<EventEdgeEntity>): String {
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
        val nodes = eventRepository.getAllEventNodes()
        val edges = eventRepository.getAllEventEdges()
        val json = convertToJsonVector(nodes, edges)
        _graphData.value = json
    }

    private fun reloadSimMatrix(
        repository: EventRepository,
        simMatrix: MutableMap<Pair<Long, Long>, Float>,
        newEventMap: Map<String, String>,
    ) {
        _simMatrix = updateSemanticSimilarityMatrix(repository, simMatrix, newEventMap)
    }

    private fun createFullGraph(nodes: List<EventNodeEntity>, edges: List<EventEdgeEntity>) {
        val json = convertToJsonVector(nodes, edges)
        _graphData.value = json
    }

    private fun createFilteredGraph(nodes: List<EventNodeEntity>, edges: List<EventEdgeEntity>) {
        val json = convertToJsonVector(nodes, edges)
        _filteredGraphData.value = json
    }

    // Function 1: Predict missing properties
    fun fillMissingLinks() {

        // Creating updated graph
        val (newEdges, response) = predictMissingProperties(eventRepository, simMatrix)
        val nodes = eventRepository.getAllEventNodes()
        val edges = eventRepository.getAllEventEdges() + newEdges
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

        viewModelScope.launch {
            val normalizedMap = map.filterValues { it.isNotBlank() }
            if (normalizedMap.isEmpty()) {
                return@launch
            }

            // Update created event list for UI
            var currentString = _createdEvent.value
            currentString = (Event(normalizedMap).toString())
            _createdEvent.value = currentString

            val noKeyTypes = normalizedMap.keys.none { it in SchemaKeyNodes }

            // For entries with no key nodes
            if (noKeyTypes) {
                val (nodes, edges, result) = recommendEventsForProps(normalizedMap, eventRepository, queryKey)
                if (nodes == null || edges == null) {
                    _uiEvent.trySend(UiEvent.ShowSnackbar("No similar events found."))
                } else {
                    createFilteredGraph(nodes, edges)
                }
                buildApiResponseFromResult(result)
                return@launch
            }

            // Check if its a duplicate event
            val (isDuplicateEvent, duplicateNode) = detectDuplicateEvent(normalizedMap)

            // Action based on type of input
            val (newEventNodes, filteredSimMatrix) = prepareNewEventNodesAndMatrix(
                isDuplicateEvent = isDuplicateEvent,
                duplicateNode = duplicateNode,
                isQuery = isQuery,
                normalizedMap = normalizedMap,
                vectorRepository = eventRepository,
                simMatrix = simMatrix,
                reloadGraphData = { reloadGraphData() },
                reloadSimMatrix = { repo, matrix, map -> reloadSimMatrix(repo, matrix, map) }
            )

            // Get recommendations and results
            val (nodes, edges, result) =
                if (isQuery && !isDuplicateEvent) {
                    recommendEventForEvent(normalizedMap, eventRepository, filteredSimMatrix, newEventNodes, queryKey, true)
                } else {
                    recommendEventForEvent(normalizedMap, eventRepository, simMatrix, newEventNodes, queryKey, false)
                }

            // Create update graph
            createFilteredGraph(nodes, edges)

            // Create API response
            buildApiResponseFromResult(result)
        }
        return
    }


    // Function 4: Find Patterns/Clusters
    fun findGraphRelations() {
        val response = findPatterns(eventRepository, simMatrix)

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
            Pair<Boolean, EventNodeEntity?> {

        val (duplicateNode, response) = detectReplicateInput(normalizedMap, eventRepository)
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

        val (nodes, edges, result) = findRelevantIncidentsUseCase(
            normalizedMap, eventRepository, simMatrix
        )

        buildApiResponseFromResult(result)
        createFilteredGraph(nodes, edges)
    }
}
