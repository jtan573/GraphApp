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
        targetEventType: SchemaKeyEventTypeNames?,
        insightCategory: InsightCategory?,
    ):  Map<SchemaKeyEventTypeNames, List<EventDetails>> {

        val response = when (insightCategory) {
            InsightCategory.SUSPICIOUS -> findRelatedSuspiciousEventsUseCase(
                eventInput = EventDetailData(
                    whatValue = eventDetails.whatValue,
                    whenValue = eventDetails.whenValue,
                    whereValue = eventDetails.whereValue,
                    howValue = eventDetails.howValue
                ),
                eventRepository = graph.eventRepository,
                embeddingRepository = graph.embeddingRepository
            )

            InsightCategory.ALERT -> fetchRelevantEventsByTargetType(
                statusEventMap = buildMap<GraphSchema.SchemaEventTypeNames, String> {
                    GraphSchema.SchemaEventTypeNames.WHERE to (eventDetails.whereValue ?: "")
                },
                eventRepository = graph.eventRepository,
                embeddingRepository = graph.embeddingRepository,
                sourceEventType = eventType,
                targetEventType = targetEventType,
                activeNodesOnly = false
            )

            else -> fetchRelevantEventsByTargetType(
                statusEventMap = buildMap<GraphSchema.SchemaEventTypeNames, String> {
                    when (eventType) {
                        SchemaKeyEventTypeNames.INCIDENT -> put(GraphSchema.SchemaEventTypeNames.INCIDENT, eventDetails.whatValue ?: "")
                        SchemaKeyEventTypeNames.IMPACT -> put(GraphSchema.SchemaEventTypeNames.IMPACT, eventDetails.whatValue ?: "")
                        SchemaKeyEventTypeNames.TASK -> put(GraphSchema.SchemaEventTypeNames.TASK, eventDetails.whatValue ?: "")
                        SchemaKeyEventTypeNames.OUTCOME -> put(GraphSchema.SchemaEventTypeNames.OUTCOME, eventDetails.whatValue ?: "")
                    }
                    put(GraphSchema.SchemaEventTypeNames.WHO, (eventDetails.whoValue ?: ""))
                    put(GraphSchema.SchemaEventTypeNames.WHEN, (eventDetails.whenValue ?: ""))
                    put(GraphSchema.SchemaEventTypeNames.WHERE, (eventDetails.whereValue ?: ""))
                    put(GraphSchema.SchemaEventTypeNames.WHY, (eventDetails.whyValue ?: ""))
                    put(GraphSchema.SchemaEventTypeNames.HOW, (eventDetails.howValue ?: ""))
                },
                eventRepository = graph.eventRepository,
                embeddingRepository = graph.embeddingRepository,
                sourceEventType = eventType,
                targetEventType = targetEventType,
                activeNodesOnly = false
            )
        }

        return response
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

    override suspend fun findThreatResponse(
        incidentEventInput: EventDetailData,
        taskEventInput: EventDetailData
    ): ThreatAlertResponse {
        val incidentResponse = fetchResponseToThreatIncidentUseCase(
            incidentEventInput, taskEventInput, graph.embeddingRepository, graph.eventRepository
        )
        return incidentResponse
    }

    override fun queryUserActions(userIdentifier: String): Map<Long, String> {
        val appActionIds = graph.userActionRepository.getUserNodeByIdentifier(userIdentifier)?.actionsTaken
        val userNode = graph.eventRepository.getEventNodeByNameAndType(
            inputName = userIdentifier, inputType = GraphSchema.SchemaEventTypeNames.WHO.key
        )
        val tasksPerformed = if (userNode != null) {
            graph.eventRepository.getNeighborsOfEventNodeById(userNode.id)
                .filter { it.type == SchemaKeyEventTypeNames.TASK.key }
        } else {
            null
        }

        val actionsMap = mutableMapOf<Long, String>()

        appActionIds?.forEach { actionId ->
            val actionNode = graph.userActionRepository.getActionNodeById(actionId)
            if (actionNode != null) {
                actionsMap.put(actionNode.timestamp, actionNode.actionName)
            }
        }
        tasksPerformed?.forEach { task ->
            val taskTime = graph.eventRepository.getNeighborsOfEventNodeById(task.id)
                .single { it.type == GraphSchema.SchemaEventTypeNames.WHEN.key}.name
            actionsMap.put(taskTime.toLong(), task.name)
        }
        return actionsMap
    }
}