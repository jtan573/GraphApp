package com.example.graphapp.core.services.kgraph.query

import com.example.graphapp.core.analyser.computeSimilarAndRelatedEvents
import com.example.graphapp.core.schema.GraphSchema
import com.example.graphapp.core.schema.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.core.model.dto.EventDetailData
import com.example.graphapp.core.model.dto.EventDetails
import com.example.graphapp.core.model.dto.ThreatAlertResponse
import com.example.graphapp.core.services.kgraph.GraphAccess
import com.example.graphapp.core.services.kgraph.query.QueryService.InsightCategory
import com.example.graphapp.core.usecases.fetchResponseToThreatIncidentUseCase
import com.example.graphapp.core.usecases.findAffectedRouteStationsByLocUseCase
import com.example.graphapp.core.usecases.findRelatedSuspiciousEventsUseCase
import com.example.graphapp.core.usecases.findRelevantPersonnelByLocationUseCase
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

            InsightCategory.ALERT -> computeSimilarAndRelatedEvents(
                newEventMap = buildMap<GraphSchema.SchemaEventTypeNames, String> {
                    put(GraphSchema.SchemaEventTypeNames.WHERE, (eventDetails.whereValue ?: ""))
                },
                eventRepository = graph.eventRepository,
                embeddingRepository = graph.embeddingRepository,
                sourceEventType = eventType,
                targetEventType = targetEventType,
                activeNodesOnly = false
            )

            else -> {
                val keyEventPair = when (eventType) {
                    SchemaKeyEventTypeNames.INCIDENT -> (GraphSchema.SchemaEventTypeNames.INCIDENT to (eventDetails.whatValue?: ""))
                    SchemaKeyEventTypeNames.IMPACT -> (GraphSchema.SchemaEventTypeNames.IMPACT to (eventDetails.whatValue?: ""))
                    SchemaKeyEventTypeNames.TASK -> (GraphSchema.SchemaEventTypeNames.TASK to (eventDetails.whatValue?: ""))
                    SchemaKeyEventTypeNames.OUTCOME -> (GraphSchema.SchemaEventTypeNames.OUTCOME to (eventDetails.whatValue?: ""))
                }
                val eventMap = when (insightCategory) {
                    InsightCategory.ALL -> buildMap<GraphSchema.SchemaEventTypeNames, String> {
                        put(keyEventPair.first, keyEventPair.second)
                        put(GraphSchema.SchemaEventTypeNames.WHO, (eventDetails.whoValue ?: ""))
                        put(GraphSchema.SchemaEventTypeNames.WHEN, (eventDetails.whenValue ?: ""))
                        put(GraphSchema.SchemaEventTypeNames.WHERE, (eventDetails.whereValue ?: ""))
                        put(GraphSchema.SchemaEventTypeNames.WHY, (eventDetails.whyValue ?: ""))
                        put(GraphSchema.SchemaEventTypeNames.HOW, (eventDetails.howValue ?: ""))
                    }
                    InsightCategory.WHO -> buildMap<GraphSchema.SchemaEventTypeNames, String> {
                        put(keyEventPair.first, keyEventPair.second)
                        put(GraphSchema.SchemaEventTypeNames.WHO, (eventDetails.whoValue ?: ""))
                    }
                    InsightCategory.WHEN -> buildMap<GraphSchema.SchemaEventTypeNames, String> {
                        put(keyEventPair.first, keyEventPair.second)
                        put(GraphSchema.SchemaEventTypeNames.WHEN, (eventDetails.whenValue ?: ""))
                    }
                    InsightCategory.WHERE -> buildMap<GraphSchema.SchemaEventTypeNames, String> {
                        put(keyEventPair.first, keyEventPair.second)
                        put(GraphSchema.SchemaEventTypeNames.WHERE, (eventDetails.whereValue ?: ""))
                    }
                    InsightCategory.WHY -> buildMap<GraphSchema.SchemaEventTypeNames, String> {
                        put(keyEventPair.first, keyEventPair.second)
                        put(GraphSchema.SchemaEventTypeNames.WHY, (eventDetails.whyValue ?: ""))
                    }
                    InsightCategory.HOW -> buildMap<GraphSchema.SchemaEventTypeNames, String> {
                        put(keyEventPair.first, keyEventPair.second)
                        put(GraphSchema.SchemaEventTypeNames.HOW, (eventDetails.howValue ?: ""))
                    }
                    else -> error("Incorrect input.")
                }
                computeSimilarAndRelatedEvents(
                    newEventMap = eventMap,
                    eventRepository = graph.eventRepository,
                    embeddingRepository = graph.embeddingRepository,
                    sourceEventType = eventType,
                    targetEventType = targetEventType,
                    activeNodesOnly = false
                )
            }
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

    override suspend fun checkRouteIntegrity(routeCoordinates: List<String>): Map<GraphSchema.DisruptionCause, List<EventDetails>>? {
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
                actionsMap.put(actionNode.timestamp, actionNode.action)
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