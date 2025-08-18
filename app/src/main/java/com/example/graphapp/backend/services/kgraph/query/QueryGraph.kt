package com.example.graphapp.backend.services.kgraph.query

import com.example.graphapp.backend.dto.GraphSchema
import com.example.graphapp.backend.services.kgraph.GraphAccess
import com.example.graphapp.backend.services.kgraph.nlp.NlpService
import com.example.graphapp.backend.services.kgraph.query.QueryService.InsightCategory
import com.example.graphapp.backend.usecases.fetchRelevantEventsByTargetType
import com.example.graphapp.backend.usecases.fetchResponseToThreatIncidentUseCase
import com.example.graphapp.backend.usecases.findAffectedRouteStationsByLocUseCase
import com.example.graphapp.backend.usecases.findRelatedSuspiciousEventsUseCase
import com.example.graphapp.backend.usecases.findRelevantPersonnelByLocationUseCase
import com.example.graphapp.data.api.DisruptionCause
import com.example.graphapp.data.api.EventDetailData
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.api.EventType
import com.example.graphapp.data.api.ThreatAlertResponse
import com.example.graphapp.data.db.UserNodeEntity
import jakarta.inject.Inject

class QueryGraph @Inject constructor(
    private val graph: GraphAccess,          // reuse repositories here
    private val nlpManager: NlpService // keep this for parsing, expansion, ranking, etc.
) : QueryService {

    override fun queryNaturalLanguage(query: String): Map<String, Float> {
        TODO("Not yet implemented")
    }

    override suspend fun querySimilarEvents(
        eventType: EventType,
        eventDetails: EventDetailData,
        targetEventType: EventType?
    ):  Map<EventType, List<EventDetails>> {

        val response = fetchRelevantEventsByTargetType(
            statusEventMap = buildMap<GraphSchema.PropertyNames, String> {
                when (eventType) {
                    EventType.INCIDENT -> put(GraphSchema.PropertyNames.INCIDENT, eventDetails.whatValue ?: "")
                    EventType.IMPACT -> put(GraphSchema.PropertyNames.IMPACT, eventDetails.whatValue ?: "")
                    EventType.TASK -> put(GraphSchema.PropertyNames.TASK, eventDetails.whatValue ?: "")
                    EventType.OUTCOME -> put(GraphSchema.PropertyNames.OUTCOME, eventDetails.whatValue ?: "")
                }
                GraphSchema.PropertyNames.WHO to (eventDetails.whoValue ?: "")
                GraphSchema.PropertyNames.WHEN to (eventDetails.whenValue ?: "")
                GraphSchema.PropertyNames.WHERE to (eventDetails.whereValue ?: "")
                GraphSchema.PropertyNames.WHY to (eventDetails.whyValue ?: "")
                GraphSchema.PropertyNames.HOW to (eventDetails.howValue ?: "")
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
        eventType: EventType?,
        inputPropertyType: InsightCategory?,
        inputValue: String,
        targetEventType: EventType?
    ): Map<EventType, List<EventDetails>> {
        val response = fetchRelevantEventsByTargetType(
            statusEventMap = buildMap<GraphSchema.PropertyNames, String> {
                if (eventType != null) {
                    when (eventType) {
                        EventType.INCIDENT -> put(GraphSchema.PropertyNames.INCIDENT, inputValue)
                        EventType.IMPACT -> put(GraphSchema.PropertyNames.IMPACT, inputValue)
                        EventType.TASK -> put(GraphSchema.PropertyNames.TASK, inputValue)
                        EventType.OUTCOME -> put(GraphSchema.PropertyNames.OUTCOME, inputValue)
                    }
                } else {
                    if (inputPropertyType != null) {
                        when (inputPropertyType) {
                            InsightCategory.WHO -> put(GraphSchema.PropertyNames.WHO, inputValue)
                            InsightCategory.WHEN -> put(GraphSchema.PropertyNames.WHEN, inputValue)
                            InsightCategory.WHERE -> put(GraphSchema.PropertyNames.WHERE, inputValue)
                            InsightCategory.WHY -> put(GraphSchema.PropertyNames.WHY, inputValue)
                            InsightCategory.HOW -> put(GraphSchema.PropertyNames.HOW, inputValue)
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
        )
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

    override suspend fun findThreatAlertAndResponse(eventInput: EventDetailData): ThreatAlertResponse {
        val incidentResponse = fetchResponseToThreatIncidentUseCase(
            eventInput, graph.userActionRepository, graph.embeddingRepository, graph.eventRepository
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