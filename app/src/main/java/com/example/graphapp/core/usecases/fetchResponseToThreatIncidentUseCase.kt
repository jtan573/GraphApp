package com.example.graphapp.core.usecases

import com.example.graphapp.core.analyser.computeSimilarAndRelatedEvents
import com.example.graphapp.core.schema.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.core.schema.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.core.model.dto.EventDetailData
import com.example.graphapp.core.model.dto.EventDetails
import com.example.graphapp.core.model.dto.ThreatAlertResponse
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository

/**
 * Implemented for Use Case 1: Threat Alert and Response
 * 1. Retrieves possible impacts from similar incidents in database.
 * 2. Retrieves instructions from similar tasks in database.
 */
suspend fun fetchResponseToThreatIncidentUseCase(
    incidentEventInput: EventDetailData,
    taskEventInput: EventDetailData?,
    embeddingRepository: EmbeddingRepository,
    eventRepository: EventRepository,
): ThreatAlertResponse {

    // Response 1: Similar Incidents -> What are the possible impacts?
    val statusEventMap = mutableMapOf<SchemaEventTypeNames, String>()
    incidentEventInput.whatValue?.map { statusEventMap.put(SchemaEventTypeNames.INCIDENT, incidentEventInput.whatValue) }
    incidentEventInput.howValue?.map { statusEventMap.put(SchemaEventTypeNames.HOW, incidentEventInput.howValue) }

    val incidentResults = computeSimilarAndRelatedEvents(
        newEventMap = statusEventMap,
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        sourceEventType = SchemaKeyEventTypeNames.INCIDENT,
        targetEventType = SchemaKeyEventTypeNames.INCIDENT,
        activeNodesOnly = false
    )
    val similarIncidents = if (incidentResults.isNotEmpty()) {
        incidentResults[SchemaKeyEventTypeNames.INCIDENT]?.map { it }
    } else { null }

    val potentialImpacts = mutableMapOf<Long, List<String>>()
    if (!similarIncidents.isNullOrEmpty()) {
        similarIncidents.forEach { simIncident ->
            val impacts = eventRepository.getNeighborsOfEventNodeById(simIncident.eventId)
                .filter { it.type == SchemaKeyEventTypeNames.IMPACT.key }.map { it.name }
            potentialImpacts.put(simIncident.eventId, impacts)
        }
    } else { null }


    // Response 2: Similar Tasks
    val potentialTasks = mutableListOf<EventDetails>()
    if (taskEventInput != null) {
        val taskStatusEventMap = mutableMapOf<SchemaEventTypeNames, String>()
        taskEventInput.whatValue?.map { statusEventMap.put(SchemaEventTypeNames.TASK, taskEventInput.whatValue) }
        taskEventInput.whyValue?.map { statusEventMap.put(SchemaEventTypeNames.WHY, taskEventInput.whyValue) }
        val taskResults = computeSimilarAndRelatedEvents(
            newEventMap = taskStatusEventMap,
            eventRepository = eventRepository,
            embeddingRepository = embeddingRepository,
            sourceEventType = SchemaKeyEventTypeNames.TASK,
            targetEventType = SchemaKeyEventTypeNames.TASK,
            activeNodesOnly = false
        )
        val predictedTasks = if (taskResults.isNotEmpty()) {
            taskResults[SchemaKeyEventTypeNames.TASK]?.map { it }
        } else { null }
        if (predictedTasks != null) {
            potentialTasks.addAll(predictedTasks)
        }
    }

    // Create function response
    val incidentResponse = ThreatAlertResponse(
        potentialImpacts = potentialImpacts,
        potentialTasks = potentialTasks,
        similarIncidents = similarIncidents
    )

    return incidentResponse
}