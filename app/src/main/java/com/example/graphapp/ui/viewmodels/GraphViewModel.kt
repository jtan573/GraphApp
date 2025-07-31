package com.example.graphapp.ui.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.api.buildApiResponseFromResult
import com.example.graphapp.data.db.ActionEdgeEntity
import com.example.graphapp.data.db.ActionNodeEntity
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.db.UserNodeEntity
import com.example.graphapp.data.schema.UiEvent
import com.example.graphapp.data.local.detectReplicateInput
import com.example.graphapp.data.local.findPatterns
import com.example.graphapp.data.local.initialiseSemanticSimilarityMatrix
import com.example.graphapp.data.local.predictMissingProperties
import com.example.graphapp.data.local.updateSemanticSimilarityMatrix
import com.example.graphapp.data.repository.DictionaryRepository
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.PosTaggerRepository
import com.example.graphapp.data.repository.UserActionRepository
import com.example.graphapp.data.schema.QueryResult
import com.example.graphapp.data.schema.QueryResult.IncidentResponse
import com.example.graphapp.domain.usecases.findAffectedRouteStationsByLocUseCase
import com.example.graphapp.domain.usecases.findRelatedSuspiciousEventsUseCase
import com.example.graphapp.domain.usecases.findRelevantPersonnelByLocationUseCase
import com.example.graphapp.domain.usecases.findThreatResponses
import com.google.gson.Gson
import edu.stanford.nlp.tagger.maxent.MaxentTagger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.Float
import kotlin.collections.map
import kotlin.text.isNotBlank
import java.io.File


class GraphViewModel(application: Application) : AndroidViewModel(application) {

    private val embeddingRepository = EmbeddingRepository(application)
    private val sentenceEmbedding = embeddingRepository.getSentenceEmbeddingModel()
    private val dictionaryRepository = DictionaryRepository(sentenceEmbedding)
    private val eventRepository = EventRepository(sentenceEmbedding, embeddingRepository, dictionaryRepository)
    private val userActionRepository = UserActionRepository(sentenceEmbedding)
    private val posTaggerRepository = PosTaggerRepository(application)

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
    private val _createdEvent = MutableStateFlow(mapOf<String, String>())
    val createdEvent: StateFlow<Map<String, String>> = _createdEvent

    private val _queryResults = MutableStateFlow<QueryResult?>(null)
    val queryResults: StateFlow<QueryResult?> = _queryResults

    // ---------- Personnel Query States ----------
    private val _relevantContactState = MutableStateFlow<Map<UserNodeEntity, Int>?>(null)
    val relevantContactState: StateFlow<Map<UserNodeEntity, Int>?> = _relevantContactState

    private val _allActiveUsers = MutableStateFlow(listOf<UserNodeEntity>())
    val allActiveUsers: StateFlow<List<UserNodeEntity>> = _allActiveUsers

    // ---------- Snackbar States ----------
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            embeddingRepository.initializeEmbedding()
            dictionaryRepository.initialiseDictionaryRepository()
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

            // Users
            _allActiveUsers.value = userActionRepository.getAllUserNodesWithoutEmbedding()

            // Testing
            posTaggerRepository.initialisePosTagger()
            Log.d("TAGGING", posTaggerRepository.tagText("The quick brown fox jumps over the lazy dog"))
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

    // Functions for Use Case 1: Find relevant personnel
    fun findRelevantPersonnelOnDemand(eventLocation: String, eventDescription: String) {
        viewModelScope.launch {
            val contactsFound = findRelevantPersonnelByLocationUseCase(
                userActionRepository = userActionRepository,
                embeddingRepository = embeddingRepository,
                threatLocation = eventLocation,
                threatDescription = eventDescription,
                radiusInMeters = 3000f
            )
            _relevantContactState.value = contactsFound
        }
    }

    // Function for Use Case 2: Helper to get Sample Data for Threat Detection
    fun getDataForThreatDetectionUseCase(identifiers: List<String>): List<UserNodeEntity> {
        val listOfUsers = mutableListOf<UserNodeEntity>()
        for (id in identifiers) {
            listOfUsers.add(userActionRepository.getUserNodeByIdentifier(id)!!)
        }
        return listOfUsers
    }
    fun findThreatAlertAndResponse(map: Map<String, String>) {
        viewModelScope.launch {
            val normalizedMap = map.filterValues { it.isNotBlank() }
            if (normalizedMap.isEmpty()) { return@launch }

            val incidentResponse = findThreatResponses(
                normalizedMap, userActionRepository, embeddingRepository, eventRepository, simMatrix
            )
            _queryResults.value = incidentResponse
            _createdEvent.value = normalizedMap
        }
    }

    // Function for Use Case 3: Suspicious Behaviour Detection
    suspend fun findDataIndicatingSuspiciousBehaviour(map: Map<String, String>): QueryResult {
        val normalizedMap = map.filterValues { it.isNotBlank() }
        if (normalizedMap.isEmpty()) { return IncidentResponse() }

        val incidentResponse = findRelatedSuspiciousEventsUseCase(
            normalizedMap, eventRepository, embeddingRepository
        )

        _queryResults.value = incidentResponse
        _createdEvent.value = normalizedMap
        return incidentResponse
    }

    // Function for Use Case 3: Helper to get Sample Data for Suspicious Behaviour Detection
    fun getDataForSuspiciousBehaviourUseCase(events: List<String>): List<Map<String, String>> {
        val listOfEvents = mutableListOf<Map<String, String>>()
        for (eventName in events) {
            val eventMap = mutableMapOf<String, String>()
            val eventNode = eventRepository.getEventNodeByNameAndType(eventName, "Incident")
            if (eventNode != null) {
                eventMap.put(eventNode.type, eventNode.name)
                val neighbours = eventRepository.getNeighborsOfEventNodeById(eventNode.id)
                neighbours.forEach { neighbour ->
                    eventMap.put(neighbour.type, neighbour.name)
                }
            }
            listOfEvents.add(eventMap)
        }
        return listOfEvents
    }

    // Function for Use Case 4: Route Integrity Check
    suspend fun findAffectedRouteStationsByLocation(locations: List<String>) {
        val results = findAffectedRouteStationsByLocUseCase(
            eventRepository = eventRepository,
            embeddingRepository = embeddingRepository,
            routeStations = locations,
        )
        _queryResults.value =  IncidentResponse(incidentsAffectingStations = results)
    }




    /* ----------------------------
                EXTRAS
    ------------------------------- */
    // Function 2/3/5: Predict Top Relationships based on Incoming Event/Detect input anomaly
//    fun provideEventRecommendation(map: Map<String, String>, isQuery: Boolean, queryKey: String? = null) {
//
//        viewModelScope.launch {
//            val normalizedMap = map.filterValues { it.isNotBlank() }
//            if (normalizedMap.isEmpty()) {
//                return@launch
//            }
//
//            // Update created event list for UI
//            _createdEvent.value = normalizedMap
//
//            val noKeyTypes = normalizedMap.keys.none { it in SchemaKeyNodes }
//            if (noKeyTypes) {
//                val (nodes, edges, result) = recommendEventsForProps(normalizedMap, eventRepository, embeddingRepository, queryKey)
//                if (nodes == null || edges == null) {
//                    _uiEvent.trySend(UiEvent.ShowSnackbar("No similar events found."))
//                } else {
//                    createFilteredEventGraph(nodes, edges)
//                }
//                buildApiResponseFromResult(result)
//                return@launch
//            }
//
//            // Check if its a duplicate event
//            val (isDuplicateEvent, duplicateNode) = detectDuplicateEvent(normalizedMap)
//
//            // Action based on type of input
//            val (newEventNodes, filteredSimMatrix) = prepareNewEventNodesAndMatrix(
//                isDuplicateEvent = isDuplicateEvent,
//                duplicateNode = duplicateNode,
//                isQuery = isQuery,
//                normalizedMap = normalizedMap,
//                eventRepository = eventRepository,
//                embeddingRepository = embeddingRepository,
//                simMatrix = simMatrix,
//                reloadGraphData = { reloadEventGraphData() },
//                reloadSimMatrix = { matrix, map -> reloadSimMatrix(matrix, map) }
//            )
//
//            // Get recommendations and results
//            val (nodes, edges, result) =
//                if (isQuery && !isDuplicateEvent) {
//                    recommendEventForEvent(normalizedMap, eventRepository, filteredSimMatrix, newEventNodes, queryKey, true)
//                } else {
//                    recommendEventForEvent(normalizedMap, eventRepository, simMatrix, newEventNodes, queryKey, false)
//                }
//
//            // Create updated graph
//            createFilteredEventGraph(nodes, edges)
//            buildApiResponseFromResult(result)
//        }
//        return
//    }
}
