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
import com.example.graphapp.backend.model.dto.EventDetailData
import com.example.graphapp.backend.model.dto.EventDetails
import com.example.graphapp.backend.model.dto.ThreatAlertResponse
import com.example.graphapp.backend.services.kgraph.KGraphService
import com.example.graphapp.backend.services.kgraph.ViewModelManager
import com.example.graphapp.backend.services.kgraph.admin.AdminService
import com.example.graphapp.backend.services.kgraph.query.QueryService
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
    private val manager: ViewModelManager
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
    private val _threatAlertCreatedEvent = MutableStateFlow(mapOf<SchemaEventTypeNames, String>())
    val threatAlertCreatedEvent: StateFlow<Map<SchemaEventTypeNames, String>> = _threatAlertCreatedEvent

    // SuspiciousDetectionUseCase
    private val _suspiciousDetectionResults = MutableStateFlow<ThreatAlertResponse?>(null)
    val suspiciousDetectionResults: StateFlow<ThreatAlertResponse?> = _suspiciousDetectionResults
    private val _suspiciousDetectionCreatedEvent = MutableStateFlow(mapOf<SchemaEventTypeNames, String>())
    val suspiciousDetectionCreatedEvent: StateFlow<Map<SchemaEventTypeNames, String>> =
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
            actionNodes.map { mapOf("id" to it.action, "type" to "Action") }.toMutableList()
        val nodeList = userNodeList + actionNodeList

        val edgeList = edges.map { edge ->
            val source = if (edge.fromNodeType == "User") {
                userNodes.find { it.id == edge.fromNodeId }?.identifier
            } else {
                actionNodes.find { it.id == edge.fromNodeId }?.action
            }
            val target = actionNodes.find { it.id == edge.toNodeId }?.action
            mapOf("source" to source, "target" to target, "label" to "")
        }

        val json = mapOf("nodes" to nodeList, "links" to edgeList)
        return gson.toJson(json)
    }

    suspend fun createFullEventGraph() {
        manager.queryService.ensureReady()
        val (eventNodes, eventEdges) = manager.adminService.retrieveEventNodesAndEdges()
        val eventJson = convertToJsonEvent(eventNodes, eventEdges)
        _eventGraphData.value = eventJson
    }

    suspend fun createFullPersonnelGraph() {
        manager.adminService.ensureReady()
        val (userNodes, actionNodes, actionEdges) = manager.adminService.retrievePersonnelNodesAndEdges()
        val userJson = convertToJsonUser(userNodes, actionNodes, actionEdges)
        _userGraphData.value = userJson
        _allActiveUsers.value = manager.adminService.retrieveAllActiveUsers()
    }


    // Functions for Use Case 1: Find relevant personnel
    override suspend fun findRelevantPersonnelOnDemand(
        inputLocation: String,
        inputDescription: String
    ) {
        viewModelScope.launch {
            val response = manager.queryService.findRelevantPersonnel(
                inputLocation, inputDescription
            )
            _relevantContactState.value = response
        }
    }

    override suspend fun findThreatAlertAndResponse(
        incidentInputMap: Map<SchemaEventTypeNames, String>,
        taskInputMap: Map<SchemaEventTypeNames, String>,
    ) {
        val incidentNormalizedMap = incidentInputMap.filterValues { it.isNotBlank() }
        val taskNormalizedMap = taskInputMap.filterValues { it.isNotBlank() }
        if (incidentNormalizedMap.isEmpty() && taskNormalizedMap.isEmpty()) {
            return
        }

        val response = manager.queryService.findThreatResponse(
            incidentEventInput = EventDetailData(
                whoValue = incidentInputMap[SchemaEventTypeNames.WHO],
                whatValue = incidentInputMap[SchemaEventTypeNames.INCIDENT],
                whenValue = incidentInputMap[SchemaEventTypeNames.WHEN],
                whereValue = incidentInputMap[SchemaEventTypeNames.WHERE],
                howValue = incidentInputMap[SchemaEventTypeNames.HOW],
                whyValue = incidentInputMap[SchemaEventTypeNames.WHY],
            ),
            taskEventInput =
                EventDetailData(
                    whoValue = taskInputMap[SchemaEventTypeNames.WHO],
                    whatValue = taskInputMap[SchemaEventTypeNames.INCIDENT],
                    whenValue = taskInputMap[SchemaEventTypeNames.WHEN],
                    whereValue = taskInputMap[SchemaEventTypeNames.WHERE],
                    howValue = taskInputMap[SchemaEventTypeNames.HOW],
                    whyValue = taskInputMap[SchemaEventTypeNames.WHY],
                )
        )
        _threatAlertResults.value = response
        _threatAlertCreatedEvent.value = incidentNormalizedMap
    }

    // Function for Use Case 3: Suspicious Behaviour Detection
    override suspend fun findRelevantSuspiciousEvents(inputMap: Map<SchemaEventTypeNames, String>) {
        val normalizedMap = inputMap.filterValues { it.isNotBlank() }
        if (normalizedMap.isEmpty()) {
            return
        }

        val response = manager.queryService.querySimilarEvents(
            eventType = SchemaKeyEventTypeNames.INCIDENT,
            eventDetails = EventDetailData(
                whoValue = inputMap[SchemaEventTypeNames.WHO],
                whatValue = inputMap[SchemaEventTypeNames.INCIDENT],
                whenValue = inputMap[SchemaEventTypeNames.WHEN],
                whereValue = inputMap[SchemaEventTypeNames.WHERE],
                howValue = inputMap[SchemaEventTypeNames.HOW],
                whyValue = inputMap[SchemaEventTypeNames.WHY],
            ),
            targetEventType = SchemaKeyEventTypeNames.INCIDENT,
            insightCategory = QueryService.InsightCategory.SUSPICIOUS
        )
        _suspiciousDetectionResults.value = ThreatAlertResponse(similarIncidents = response[SchemaKeyEventTypeNames.INCIDENT])
        _suspiciousDetectionCreatedEvent.value = normalizedMap
    }

    // Function for Use Case 4: Route Integrity Check
    override suspend fun findAffectedRouteStationsByLocation(locations: List<String>) {
        if (locations.isNotEmpty()) {
            val response = manager.queryService.checkRouteIntegrity(locations)
            _routeIntegrityResults.value =
                ThreatAlertResponse(incidentsAffectingStations = response)
        }
    }

    // General Function
    override suspend fun findSimilarEvents(
        inputEventType: SchemaKeyEventTypeNames,
        inputEventDetails: EventDetailData,
        targetEventType: SchemaKeyEventTypeNames?,
        insightCategory: QueryService.InsightCategory //more
    ): Map<SchemaKeyEventTypeNames, List<EventDetails>> {
        return manager.queryService.querySimilarEvents(
            eventType = inputEventType,
            eventDetails = inputEventDetails,
            targetEventType = targetEventType,
            insightCategory = insightCategory
        )
    }

    // Shift Handover
    override fun findShiftHandoverSummary(userIdentifier: String): Map<Long, String> {
        return manager.queryService.queryUserActions(userIdentifier)
    }

}