package com.example.graphapp.backend.services.kgraph.query

import android.media.metrics.Event
import com.example.graphapp.backend.core.GraphSchema
import com.example.graphapp.backend.core.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.backend.services.kgraph.GraphAccess
import com.example.graphapp.backend.services.kgraph.query.QueryService.InsightCategory
import com.example.graphapp.backend.usecases.fetchRelevantEventsByTargetType
import com.example.graphapp.backend.usecases.fetchResponseToThreatIncidentUseCase
import com.example.graphapp.backend.usecases.findAffectedRouteStationsByLocUseCase
import com.example.graphapp.backend.usecases.findRelatedSuspiciousEventsUseCase
import com.example.graphapp.backend.usecases.findRelevantPersonnelByLocationUseCase
import com.example.graphapp.data.api.DisruptionCause
import com.example.graphapp.data.api.EventDetailData
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.api.ThreatAlertResponse
import com.example.graphapp.data.db.UserNodeEntity
import jakarta.inject.Inject

class QueryGraph @Inject constructor(
    private val graph: GraphAccess,          // reuse repositories here
//    private val nlpManager: NlpService // keep this for parsing, expansion, ranking, etc.
) : QueryService {

    override suspend fun ensureReady() = graph.awaitReady()

    override fun queryNaturalLanguage(query: String): Map<String, Float> {
        TODO("Not yet implemented")
    }

    override suspend fun querySimilarEvents(
        eventType: SchemaKeyEventTypeNames,
        eventDetails: EventDetailData,
        targetEventType: SchemaKeyEventTypeNames?
    ):  Map<SchemaKeyEventTypeNames, List<EventDetails>> {

        val response = fetchRelevantEventsByTargetType(
            statusEventMap = buildMap<GraphSchema.SchemaEventTypeNames, String> {
                when (eventType) {
                    SchemaKeyEventTypeNames.INCIDENT -> put(GraphSchema.SchemaEventTypeNames.INCIDENT, eventDetails.whatValue ?: "")
                    SchemaKeyEventTypeNames.IMPACT -> put(GraphSchema.SchemaEventTypeNames.IMPACT, eventDetails.whatValue ?: "")
                    SchemaKeyEventTypeNames.TASK -> put(GraphSchema.SchemaEventTypeNames.TASK, eventDetails.whatValue ?: "")
                    SchemaKeyEventTypeNames.OUTCOME -> put(GraphSchema.SchemaEventTypeNames.OUTCOME, eventDetails.whatValue ?: "")
                }
                GraphSchema.SchemaEventTypeNames.WHO to (eventDetails.whoValue ?: "")
                GraphSchema.SchemaEventTypeNames.WHEN to (eventDetails.whenValue ?: "")
                GraphSchema.SchemaEventTypeNames.WHERE to (eventDetails.whereValue ?: "")
                GraphSchema.SchemaEventTypeNames.WHY to (eventDetails.whyValue ?: "")
                GraphSchema.SchemaEventTypeNames.HOW to (eventDetails.howValue ?: "")
            },
            eventRepository = graph.eventRepository,
            embeddingRepository = graph.embeddingRepository,
            sourceEventType = eventType,
            queryKey = targetEventType,
            activeNodesOnly = false
        )

        return response.third.predictedEvents
    }

    override suspend fun querySimilarEventsByCategory(
        eventType: SchemaKeyEventTypeNames?,
        inputPropertyType: InsightCategory?,
        inputValue: String,
        targetEventType: SchemaKeyEventTypeNames?
    ): Map<SchemaKeyEventTypeNames, List<EventDetails>> {
        val response = fetchRelevantEventsByTargetType(
            statusEventMap = buildMap<GraphSchema.SchemaEventTypeNames, String> {
                if (eventType != null) {
                    when (eventType) {
                        SchemaKeyEventTypeNames.INCIDENT -> put(GraphSchema.SchemaEventTypeNames.INCIDENT, inputValue)
                        SchemaKeyEventTypeNames.IMPACT -> put(GraphSchema.SchemaEventTypeNames.IMPACT, inputValue)
                        SchemaKeyEventTypeNames.TASK -> put(GraphSchema.SchemaEventTypeNames.TASK, inputValue)
                        SchemaKeyEventTypeNames.OUTCOME -> put(GraphSchema.SchemaEventTypeNames.OUTCOME, inputValue)
                    }
                } else {
                    if (inputPropertyType != null) {
                        when (inputPropertyType) {
                            InsightCategory.WHO -> put(GraphSchema.SchemaEventTypeNames.WHO, inputValue)
                            InsightCategory.WHEN -> put(GraphSchema.SchemaEventTypeNames.WHEN, inputValue)
                            InsightCategory.WHERE -> put(GraphSchema.SchemaEventTypeNames.WHERE, inputValue)
                            InsightCategory.WHY -> put(GraphSchema.SchemaEventTypeNames.WHY, inputValue)
                            InsightCategory.HOW -> put(GraphSchema.SchemaEventTypeNames.HOW, inputValue)
                        }
                    }
                }
            },
            eventRepository = graph.eventRepository,
            embeddingRepository = graph.embeddingRepository,
            queryKey = targetEventType,
            activeNodesOnly = false
        )
        return response.third.predictedEvents
    }

    override suspend fun findRelevantPersonnel(inputLoc: String, inputDesc: String): Map<UserNodeEntity, Int>? {
        val contactsFound = findRelevantPersonnelByLocationUseCase(
            userActionRepository = graph.userActionRepository,
            embeddingRepository = graph.embeddingRepository,
            threatLocation = inputLoc,
            threatDescription = inputDesc,
            radiusInMeters = 3000f
        )?.associate { it.first to it.second }
        return contactsFound
    }

    override suspend fun checkRouteIntegrity(routeCoordinates: List<String>): Map<DisruptionCause, List<EventDetails>>? {
        val results = findAffectedRouteStationsByLocUseCase(
            eventRepository = graph.eventRepository,
            embeddingRepository = graph.embeddingRepository,
            routeStations = routeCoordinates,
        )
        return results
    }

    override suspend fun findThreatAlertAndResponse(
        incidentEventInput: EventDetailData,
        taskEventInput: EventDetailData
    ): ThreatAlertResponse {
        val incidentResponse = fetchResponseToThreatIncidentUseCase(
            incidentEventInput, taskEventInput, graph.userActionRepository, graph.embeddingRepository, graph.eventRepository
        )
        return incidentResponse
    }

    override suspend fun findSuspiciousEventsQuery(event: EventDetailData): List<EventDetails>? {
        val incidentResponse = findRelatedSuspiciousEventsUseCase(
            eventInput = event,
            eventRepository = graph.eventRepository,
            embeddingRepository = graph.embeddingRepository
        )
        return incidentResponse
    }

}