package com.example.graphapp.ui.viewmodels

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.schema.Event
import com.example.graphapp.data.api.ApiResponse
import com.example.graphapp.data.api.ResponseData
import com.example.graphapp.data.api.buildApiResponseFromResult
import com.example.graphapp.data.db.ActionEdgeEntity
import com.example.graphapp.data.db.ActionNodeEntity
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.db.UserNodeEntity
import com.example.graphapp.data.schema.UiEvent
import com.example.graphapp.data.schema.GraphSchema.SchemaKeyNodes
import com.example.graphapp.data.local.detectReplicateInput
import com.example.graphapp.data.local.findPatterns
import com.example.graphapp.data.local.initialiseSemanticSimilarityMatrix
import com.example.graphapp.data.local.predictMissingProperties
import com.example.graphapp.data.local.recommendEventForEvent
import com.example.graphapp.data.local.recommendEventsForProps
import com.example.graphapp.data.local.updateSemanticSimilarityMatrix
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.UserActionRepository
import com.example.graphapp.domain.usecases.findAffectedEventsByLocation
import com.example.graphapp.domain.usecases.findRelevantIncidentsUseCase
import com.example.graphapp.domain.usecases.findRelevantContactsUseCase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.nio.file.attribute.AclEntryPermission
import kotlin.Float
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.text.isNotBlank

class GraphViewModel(application: Application) : AndroidViewModel(application) {

    private val embeddingRepository = EmbeddingRepository(application)
    private val sentenceEmbedding = embeddingRepository.getSentenceEmbeddingModel()
    private val eventRepository = EventRepository(sentenceEmbedding)
    private val userActionRepository = UserActionRepository(sentenceEmbedding)

    // ---------- Graph Data States ----------
    private val _eventGraphData = MutableStateFlow<String?>(null)
    val eventGraphData: StateFlow<String?> = _eventGraphData

    private val _filteredGraphData = MutableStateFlow<String?>(null)
    val filteredGraphData: StateFlow<String?> = _filteredGraphData

    private val _userGraphData = MutableStateFlow<String?>(null)
    val userGraphData: StateFlow<String?> = _userGraphData

    // ---------- Graph Logic States ----------
    private var _simMatrix: Map<Pair<Long, Long>, Float>? = null
    val simMatrix: Map<Pair<Long, Long>, Float>
        get() = _simMatrix ?: initialiseSemanticSimilarityMatrix(eventRepository, embeddingRepository).also {
            _simMatrix = it
        }

    // ---------- Event Query States ----------
    private val _createdEvent = MutableStateFlow<String>("")
    val createdEvent: StateFlow<String> = _createdEvent

    private val _detectedEvents = MutableStateFlow(mutableListOf<Triple<Long, String, String>>())
    val detectedEvents: StateFlow<List<Triple<Long, String, String>>> = _detectedEvents

    // ---------- Personnel Query States ----------
    private val _relevantContactState = MutableStateFlow(mutableMapOf<String, String>())
    val relevantContactState: StateFlow<Map<String, String>> = _relevantContactState

    private val _userContactQuery = MutableStateFlow<String>("")
    val userContactQuery: StateFlow<String> = _userContactQuery

    // ---------- Snackbar States ----------
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            embeddingRepository.initializeEmbedding()
            eventRepository.initialiseEventRepository()
            userActionRepository.initialiseUserActionRepository()

            // For event repository
            val eventNodes = eventRepository.getAllEventNodesWithoutEmbedding()
            val eventEdges = eventRepository.getAllEventEdges()
            val eventJson = convertToJsonEvent(eventNodes, eventEdges)
            _eventGraphData.value = eventJson

            // For user-action repository
            val userNodes = userActionRepository.getAllUserNodesWithoutEmbedding()
            val actionNodes = userActionRepository.getAllActionNodesWithoutEmbedding()
            val actionEdges = userActionRepository.getAllActionEdges()
            val userJson = convertToJsonUser(userNodes, actionNodes, actionEdges)
            _userGraphData.value = userJson
        }
    }

    private fun convertToJsonEvent(nodes: List<EventNodeEntity>, edges: List<EventEdgeEntity>): String {
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

    private fun convertToJsonUser(userNodes: List<UserNodeEntity>, actionNodes: List<ActionNodeEntity>, edges: List<ActionEdgeEntity>): String {
        val gson = Gson()
        val userNodeList = userNodes.map { mapOf("id" to it.identifier, "type" to "User") }.toMutableList()
        val actionNodeList = actionNodes.map { mapOf("id" to it.actionName, "type" to "Action") }.toMutableList()
        val nodeList = userNodeList + actionNodeList

        val edgeList = edges.map { edge ->
            val source = if (edge.fromNodeType == "User") {
                userNodes.find { it.id == edge.fromNodeId }?.identifier
            } else {
                actionNodes.find { it.id == edge.fromNodeId }?.actionName
            }
            val target = actionNodes.find { it.id == edge.toNodeId }?.actionName
            mapOf("source" to source, "target" to target, "label" to "")
        }

        val json = mapOf("nodes" to nodeList, "links" to edgeList)
        return gson.toJson(json)
    }

    private fun reloadEventGraphData() {
        val nodes = eventRepository.getAllEventNodesWithoutEmbedding()
        val edges = eventRepository.getAllEventEdges()
        val json = convertToJsonEvent(nodes, edges)
        _eventGraphData.value = json
    }

    private fun reloadUserGraphData() {
        val userNodes = userActionRepository.getAllUserNodes()
        val actionNodes = userActionRepository.getAllActionNodes()
        val edges = userActionRepository.getAllActionEdges()
        val json = convertToJsonUser(userNodes, actionNodes, edges)
        _userGraphData.value = json
    }

    private fun reloadSimMatrix(
        simMatrix: MutableMap<Pair<Long, Long>, Float>,
        newEventMap: Map<String, String>,
    ) {
        _simMatrix = updateSemanticSimilarityMatrix(eventRepository, embeddingRepository, simMatrix, newEventMap)
    }

    private fun createFullEventGraph(nodes: List<EventNodeEntity>, edges: List<EventEdgeEntity>) {
        val json = convertToJsonEvent(nodes, edges)
        _eventGraphData.value = json
    }

    private fun createFilteredEventGraph(nodes: List<EventNodeEntity>, edges: List<EventEdgeEntity>) {
        val json = convertToJsonEvent(nodes, edges)
        _filteredGraphData.value = json
    }

    // Function 1: Predict missing properties
    fun fillMissingLinks() {
        val (newEdges, response) = predictMissingProperties(eventRepository, simMatrix)
        val nodes = eventRepository.getAllEventNodes()
        val edges = eventRepository.getAllEventEdges() + newEdges
        createFullEventGraph(nodes, edges)

        buildApiResponseFromResult(response)
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
            _createdEvent.value = (Event(normalizedMap).toString())

            val noKeyTypes = normalizedMap.keys.none { it in SchemaKeyNodes }
            if (noKeyTypes) {
                val (nodes, edges, result) = recommendEventsForProps(normalizedMap, eventRepository, embeddingRepository, queryKey)
                if (nodes == null || edges == null) {
                    _uiEvent.trySend(UiEvent.ShowSnackbar("No similar events found."))
                } else {
                    createFilteredEventGraph(nodes, edges)
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
                eventRepository = eventRepository,
                embeddingRepository = embeddingRepository,
                simMatrix = simMatrix,
                reloadGraphData = { reloadEventGraphData() },
                reloadSimMatrix = { matrix, map -> reloadSimMatrix(matrix, map) }
            )

            // Get recommendations and results
            val (nodes, edges, result) =
                if (isQuery && !isDuplicateEvent) {
                    recommendEventForEvent(normalizedMap, eventRepository, filteredSimMatrix, newEventNodes, queryKey, true)
                } else {
                    recommendEventForEvent(normalizedMap, eventRepository, simMatrix, newEventNodes, queryKey, false)
                }

            // Create updated graph
            createFilteredEventGraph(nodes, edges)
            buildApiResponseFromResult(result)
        }
        return
    }

    // Function 4: Find Patterns/Clusters
    fun findGraphRelations() {
        val response = findPatterns(eventRepository, simMatrix)
        buildApiResponseFromResult(response)
        return
    }

    // Function 5: Detect Same Event
    suspend fun detectDuplicateEvent(normalizedMap: Map<String, String>): Pair<Boolean, EventNodeEntity?> {

        val (duplicateNode, response) = detectReplicateInput(normalizedMap, eventRepository, embeddingRepository)
        buildApiResponseFromResult(response)

        if (response.isLikelyDuplicate == true) {
            _uiEvent.trySend(UiEvent.ShowSnackbar("Very similar event(s) found."))
        }

        return response.isLikelyDuplicate to duplicateNode
    }

    // Use Case Function
    suspend fun findRelevantIncidents(map: Map<String, String>) {
        val normalizedMap = map.filterValues { it.isNotBlank() }
        if (normalizedMap.isEmpty()) return

        val (nodes, edges, result) = findRelevantIncidentsUseCase(
            normalizedMap, eventRepository, embeddingRepository, simMatrix
        )

        buildApiResponseFromResult(result)
        createFilteredEventGraph(nodes, edges)
    }

    // Find relevant personnel/contacts on-demand
    suspend fun findRelevantContacts(eventDescription: String) {
        _userContactQuery.value = eventDescription
        val contactsFound = findRelevantContactsUseCase(eventDescription, userActionRepository, embeddingRepository)
        val newMap = contactsFound.associate { (id, name, _) -> id to name }
        _relevantContactState.value = newMap.toMutableMap()
    }

    // Threat proximity alerts
    fun detectNearbyThreats(threatLocation: Location) {
        val affectedEventsList = findAffectedEventsByLocation(
            eventRepository, threatLocation, 1000f
        )

        if (affectedEventsList.isNotEmpty()) {

        }
    }
}
