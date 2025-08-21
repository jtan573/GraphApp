package com.example.graphapp.frontend.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.graphapp.data.db.ActionEdgeEntity
import com.example.graphapp.data.db.ActionNodeEntity
import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.db.UserNodeEntity
import com.example.graphapp.frontend.components.UiEvent
import com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.core.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.backend.services.kgraph.KGraphService
import com.example.graphapp.backend.services.kgraph.admin.AdminService
import com.example.graphapp.backend.services.kgraph.query.QueryService
import com.example.graphapp.data.api.EventDetailData
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.api.ThreatAlertResponse
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.String
import kotlin.collections.map
import kotlin.text.isNotBlank

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val queryService: QueryService,
    private val adminService: AdminService
) : ViewModel(), KGraphService {

    // ---------- Graph Data States ----------
    private val _eventGraphData = MutableStateFlow<String?>(null)
    val eventGraphData: StateFlow<String?> = _eventGraphData

    private val _userGraphData = MutableStateFlow<String?>(null)
    val userGraphData: StateFlow<String?> = _userGraphData

    // ---------- Event Query States ----------
    // RouteIntegrityUseCase
    private val _routeIntegrityResults = MutableStateFlow<ThreatAlertResponse?>(null)
    val routeIntegrityResults: StateFlow<ThreatAlertResponse?> = _routeIntegrityResults
    private val _routeIntegrityCreatedEvent = MutableStateFlow(mapOf<String, String>())
    val routeIntegrityCreatedEvent: StateFlow<Map<String, String>> = _routeIntegrityCreatedEvent

    // ThreatAlertUseCase
    private val _threatAlertResults = MutableStateFlow<ThreatAlertResponse?>(null)
    val threatAlertResults: StateFlow<ThreatAlertResponse?> = _threatAlertResults
    private val _threatAlertCreatedEvent = MutableStateFlow(mapOf<String, String>())
    val threatAlertCreatedEvent: StateFlow<Map<String, String>> = _threatAlertCreatedEvent

    // SuspiciousDetectionUseCase
    private val _suspiciousDetectionResults = MutableStateFlow<ThreatAlertResponse?>(null)
    val suspiciousDetectionResults: StateFlow<ThreatAlertResponse?> = _suspiciousDetectionResults
    private val _suspiciousDetectionCreatedEvent = MutableStateFlow(mapOf<String, String>())
    val suspiciousDetectionCreatedEvent: StateFlow<Map<String, String>> =
        _suspiciousDetectionCreatedEvent

    // ---------- Personnel Query States ----------
    private val _relevantContactState = MutableStateFlow<Map<UserNodeEntity, Int>?>(null)
    val relevantContactState: StateFlow<Map<UserNodeEntity, Int>?> = _relevantContactState

    private val _allActiveUsers = MutableStateFlow(listOf<UserNodeEntity>())
    val allActiveUsers: StateFlow<List<UserNodeEntity>> = _allActiveUsers

    // ---------- Snackbar States ----------
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private fun convertToJsonEvent(
        nodes: List<EventNodeEntity>,
        edges: List<EventEdgeEntity>
    ): String {
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

    private fun convertToJsonUser(
        userNodes: List<UserNodeEntity>,
        actionNodes: List<ActionNodeEntity>,
        edges: List<ActionEdgeEntity>
    ): String {
        val gson = Gson()
        val userNodeList =
            userNodes.map { mapOf("id" to it.identifier, "type" to "User") }.toMutableList()
        val actionNodeList =
            actionNodes.map { mapOf("id" to it.actionName, "type" to "Action") }.toMutableList()
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

    suspend fun createFullEventGraph() {
        queryService.ensureReady()
        val (eventNodes, eventEdges) = adminService.retrieveEventNodesAndEdges()
        val eventJson = convertToJsonEvent(eventNodes, eventEdges)
        _eventGraphData.value = eventJson
    }

    suspend fun createFullPersonnelGraph() {
        adminService.ensureReady()
        val (userNodes, actionNodes, actionEdges) = adminService.retrievePersonnelNodesAndEdges()
        val userJson = convertToJsonUser(userNodes, actionNodes, actionEdges)
        _userGraphData.value = userJson
        _allActiveUsers.value = adminService.retrieveAllActiveUsers()
    }

    // Function 1: Predict missing properties
    fun fillMissingLinks() {
        TODO("Not yet implemented.")
    }

    // Function 4: Find Patterns/Clusters
    fun findGraphRelations() {
        TODO("Not yet implemented.")
    }

    // Function 5: Detect Same Event
    fun detectDuplicateEvent(normalizedMap: Map<String, String>): Pair<Boolean, EventNodeEntity?> {
        TODO("Not yet implemented.")
    }

    // Functions for Use Case 1: Find relevant personnel
    override suspend fun findRelevantPersonnelOnDemand(
        inputLocation: String,
        inputDescription: String
    ) {
        viewModelScope.launch {
            val response = queryService.findRelevantPersonnel(
                inputLocation, inputDescription
            )
            _relevantContactState.value = response
        }
    }

    // Function for Use Case 2: Helper to get Sample Data for Threat Detection
    fun getDataForThreatDetectionUseCase(identifiers: List<String>): List<UserNodeEntity> {
        return adminService.getDataForThreatAlertUseCase(identifiers)
    }

    override suspend fun findThreatAlertAndResponse(inputMap: Map<String, String>) {
        val normalizedMap = inputMap.filterValues { it.isNotBlank() }
        if (normalizedMap.isEmpty()) {
            return
        }

        val response = queryService.findThreatAlertAndResponse(
            eventInput = EventDetailData(
                whoValue = inputMap[SchemaEventTypeNames.WHO.key],
                whatValue = inputMap[SchemaEventTypeNames.INCIDENT.key],
                whenValue = inputMap[SchemaEventTypeNames.WHEN.key],
                whereValue = inputMap[SchemaEventTypeNames.WHERE.key],
                howValue = inputMap[SchemaEventTypeNames.HOW.key],
                whyValue = inputMap[SchemaEventTypeNames.WHY.key],
            )
        )
        _threatAlertResults.value = response
        _threatAlertCreatedEvent.value = normalizedMap
    }

    // Function for Use Case 3: Suspicious Behaviour Detection
    override suspend fun findSimilarSuspiciousEventsByLocationAndApproach(inputMap: Map<String, String>) {
        val normalizedMap = inputMap.filterValues { it.isNotBlank() }
        if (normalizedMap.isEmpty()) {
            return
        }

        val response = queryService.findSuspiciousEventsQuery(
            event = EventDetailData(
                whoValue = inputMap[SchemaEventTypeNames.WHO.key],
                whatValue = inputMap[SchemaEventTypeNames.INCIDENT.key],
                whenValue = inputMap[SchemaEventTypeNames.WHEN.key],
                whereValue = inputMap[SchemaEventTypeNames.WHERE.key],
                howValue = inputMap[SchemaEventTypeNames.HOW.key],
                whyValue = inputMap[SchemaEventTypeNames.WHY.key],
            )
        )
        _suspiciousDetectionResults.value = ThreatAlertResponse(similarIncidents = response)
        _suspiciousDetectionCreatedEvent.value = normalizedMap
    }

    fun getDataForSuspiciousBehaviourUseCase(events: List<String>): List<Map<String, String>> {
        return adminService.getDataForSuspiciousEventDetectionUseCase(events)
    }

    // Function for Use Case 4: Route Integrity Check
    override suspend fun findAffectedRouteStationsByLocation(locations: List<String>) {
        if (locations.isNotEmpty()) {
            val response = queryService.checkRouteIntegrity(locations)
            _routeIntegrityResults.value =
                ThreatAlertResponse(incidentsAffectingStations = response)
        }
    }

    override suspend fun findSimilarEvents(
        givenEventType: SchemaKeyEventTypeNames,
        targetEventType: SchemaKeyEventTypeNames?,
        eventDetails: EventDetailData,
    ): Map<SchemaKeyEventTypeNames, List<EventDetails>> {
        return queryService.querySimilarEvents(
            eventType = givenEventType,
            eventDetails = eventDetails,
            targetEventType = targetEventType
        )
    }

    override suspend fun findSimilarEventsByProperty(
        inputEventType: SchemaKeyEventTypeNames?,
        targetSimilarityProperty: QueryService.InsightCategory?,
        inputPropertyValue: String,
        targetEventType: SchemaKeyEventTypeNames?
    ): Map<SchemaKeyEventTypeNames, List<EventDetails>> {
        return queryService.querySimilarEventsByCategory(
            eventType = inputEventType,
            inputPropertyType = targetSimilarityProperty,
            inputValue = inputPropertyValue,
            targetEventType = targetEventType
        )
    }
}