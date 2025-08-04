package com.example.graphapp.frontend.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.graphapp.backend.AppBackend
import com.example.graphapp.data.api.ApiRequest
import com.example.graphapp.data.api.DbAction
import com.example.graphapp.data.api.RequestData
import com.example.graphapp.data.api.ResponseStatus
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.api.buildApiResponseFromResult
import com.example.graphapp.data.db.ActionEdgeEntity
import com.example.graphapp.data.db.ActionNodeEntity
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.db.UserNodeEntity
import com.example.graphapp.backend.schema.UiEvent
import com.example.graphapp.backend.core.detectReplicateInput
import com.example.graphapp.backend.core.findPatterns
import com.example.graphapp.backend.core.initialiseSemanticSimilarityMatrix
import com.example.graphapp.backend.core.predictMissingProperties
import com.example.graphapp.backend.dto.GraphSchema.PropertyNames
import com.example.graphapp.backend.services.ApiRouter
import com.example.graphapp.backend.usecases.findAffectedRouteStationsByLocUseCase
import com.example.graphapp.backend.usecases.findRelatedSuspiciousEventsUseCase
import com.example.graphapp.backend.usecases.findRelevantPersonnelByLocationUseCase
import com.example.graphapp.backend.usecases.findThreatResponses
import com.example.graphapp.data.api.ApiResponse
import com.example.graphapp.data.api.ContactRelevantPersonnelResponse
import com.example.graphapp.data.api.EventDetailData
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.api.RequestData.EventRequestData
import com.example.graphapp.data.api.RequestData.PersonnelRequestData
import com.example.graphapp.data.api.RequestEntry
import com.example.graphapp.data.api.ResponseData.ContactPersonnelData
import com.example.graphapp.data.api.ResponseData.ThreatAlertData
import com.example.graphapp.data.api.ThreatAlertResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.Float
import kotlin.String
import kotlin.collections.map
import kotlin.text.isNotBlank


class GraphViewModel(application: Application) : AndroidViewModel(application) {

    private val backend = AppBackend(application)

    // Access backend repositories
    private val embeddingRepository = backend.embeddingRepository
    private val eventRepository = backend.eventRepository
    private val userActionRepository = backend.userActionRepository

    // ---------- Graph Data States ----------
    private val _eventGraphData = MutableStateFlow<String?>(null)
    val eventGraphData: StateFlow<String?> = _eventGraphData

    private val _filteredGraphData = MutableStateFlow<String?>(null)
    val filteredGraphData: StateFlow<String?> = _filteredGraphData

    private val _userGraphData = MutableStateFlow<String?>(null)
    val userGraphData: StateFlow<String?> = _userGraphData

    // ---------- Graph Logic States ----------
    private val _simMatrix = MutableStateFlow<Map<Pair<Long, Long>, Float>>(emptyMap())
    val simMatrix: StateFlow<Map<Pair<Long, Long>, Float>> = _simMatrix

    // ---------- Event Query States ----------
    private val _createdEvent = MutableStateFlow(mapOf<String, String>())
    val createdEvent: StateFlow<Map<String, String>> = _createdEvent

    private val _queryResults = MutableStateFlow<ThreatAlertResponse?>(null)
    val queryResults: StateFlow<ThreatAlertResponse?> = _queryResults

    // ---------- Personnel Query States ----------
    private val _relevantContactState = MutableStateFlow<ContactRelevantPersonnelResponse?>(null)
    val relevantContactState: StateFlow<ContactRelevantPersonnelResponse?> = _relevantContactState

    private val _allActiveUsers = MutableStateFlow(listOf<UserNodeEntity>())
    val allActiveUsers: StateFlow<List<UserNodeEntity>> = _allActiveUsers

    // ---------- Snackbar States ----------
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            backend.initialiseBackend()

            _simMatrix.value = initialiseSemanticSimilarityMatrix(eventRepository, embeddingRepository)

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

//    private fun reloadSimMatrix(
//        simMatrix: MutableMap<Pair<Long, Long>, Float>,
//        newEventMap: Map<String, String>,
//    ) {
//        _simMatrix = updateSemanticSimilarityMatrix(eventRepository, embeddingRepository, simMatrix, newEventMap)
//    }

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
        val (newEdges, response) = predictMissingProperties(eventRepository, simMatrix.value)
        val nodes = eventRepository.getAllEventNodes()
        val edges = eventRepository.getAllEventEdges() + newEdges
        createFullEventGraph(nodes, edges)

        buildApiResponseFromResult(response)
        return
    }

    // Function 4: Find Patterns/Clusters
    fun findGraphRelations() {
        val response = findPatterns(eventRepository, simMatrix.value)
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

    // API ROUTING
    suspend fun callBackend(inputRequest: RequestData) : ApiResponse {
        val request = ApiRequest(
            userId = "TEMP",
            timestamp = System.currentTimeMillis(),
            action = DbAction.CREATE,
            inputData = inputRequest
        )
        val apiRes = ApiRouter.handlePredict(
            request,
            embeddingRepository = embeddingRepository,
            eventRepository = eventRepository,
            userActionRepository = userActionRepository
        )
        Log.d("CHECK RESULT", "apiRes: $apiRes")
        return apiRes
    }

    // Functions for Use Case 1: Find relevant personnel
    fun findRelevantPersonnelOnDemand(inputLocation: String, inputDescription: String) {
        viewModelScope.launch {
            val apiRes = callBackend(
                PersonnelRequestData(
                    whereValue = inputLocation,
                    descValue = inputDescription
                )
            )

            if (apiRes.data is ContactPersonnelData) {
                _relevantContactState.value = apiRes.data.payload
            }
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
    fun findThreatAlertAndResponse(inputMap: Map<String, String>) {
        viewModelScope.launch {
            val normalizedMap = inputMap.filterValues { it.isNotBlank() }
            if (normalizedMap.isEmpty()) { return@launch }

            val apiRes = callBackend(
                EventRequestData(
                    eventType = "Incident",
                    details = EventDetailData(
                        whoValue = inputMap[PropertyNames.WHO.key],
                        whatValue = inputMap[PropertyNames.INCIDENT.key],
                        whenValue = inputMap[PropertyNames.WHEN.key],
                        whereValue = inputMap[PropertyNames.WHERE.key],
                        howValue = inputMap[PropertyNames.HOW.key],
                        whyValue = inputMap[PropertyNames.WHY.key]
                    )
                )
            )

            if (apiRes.data is ThreatAlertData) {
                Log.d("CHECK", "threat alert data detected")
                _queryResults.value = apiRes.data.payload
                _createdEvent.value = normalizedMap
            }
        }
    }

    // Function for Use Case 3: Suspicious Behaviour Detection
    suspend fun findDataIndicatingSuspiciousBehaviour(inputMap: Map<String, String>) {
        val normalizedMap = inputMap.filterValues { it.isNotBlank() }
        if (normalizedMap.isEmpty()) { return }

        val apiRes = callBackend(
            EventRequestData(
                eventType = "Incident",
                details = EventDetailData(
                    whoValue = inputMap[PropertyNames.WHO.key],
                    whatValue = inputMap[PropertyNames.INCIDENT.key],
                    whenValue = inputMap[PropertyNames.WHEN.key],
                    whereValue = inputMap[PropertyNames.WHERE.key],
                    howValue = inputMap[PropertyNames.HOW.key],
                    whyValue = inputMap[PropertyNames.WHY.key]
                )
            )
        )

        if (apiRes.data is ThreatAlertData) {
            _queryResults.value = apiRes.data.payload
            _createdEvent.value = normalizedMap
        }
    }

    fun getDataForSuspiciousBehaviourUseCase(events: List<String>): List<Map<String, String>> {
        val listOfEvents = mutableListOf<Map<String, String>>()
        for (eventName in events) {
            val eventMap = mutableMapOf<String, String>()
            val eventNode = eventRepository.getEventNodeByNameAndType(eventName, PropertyNames.INCIDENT.key)
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
        if (locations.isNotEmpty()) {
            val apiRes = callBackend(
                EventRequestData(
                    metadata = RequestEntry(routeCoordinates = locations)
                )
            )
            if (apiRes.data is ThreatAlertData) {
                _queryResults.value = apiRes.data.payload
            }
        }
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
