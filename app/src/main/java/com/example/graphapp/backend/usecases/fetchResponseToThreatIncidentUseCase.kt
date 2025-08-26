package com.example.graphapp.backend.usecases

import android.util.Log
import com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.core.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.backend.model.dto.EventDetailData
import com.example.graphapp.backend.model.dto.EventDetails
import com.example.graphapp.backend.model.dto.ThreatAlertResponse
import com.example.graphapp.data.db.UserNodeEntity
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.repository.UserActionRepository

// App response to INCIDENTS
suspend fun fetchResponseToThreatIncidentUseCase(
    incidentEventInput: EventDetailData,
    taskEventInput: EventDetailData?,
    embeddingRepository: EmbeddingRepository,
    eventRepository: EventRepository,
): ThreatAlertResponse {

    // Response 1: Active personnel with related specialisation
//    val threatLocation = incidentEventInput.whereValue
//    val nearbyPersonnelMap = findRelevantPersonnelByLocationUseCase(
//        userActionRepository = userActionRepository,
//        embeddingRepository = embeddingRepository,
//        threatLocation = threatLocation,
//        radiusInMeters = 3000f
//    )?.associate { it.first to it.second }


    // Response 1: Similar Incidents -> What are the possible impacts?
    val statusEventMap = mutableMapOf<SchemaEventTypeNames, String>()
    incidentEventInput.whatValue?.map { statusEventMap.put(SchemaEventTypeNames.INCIDENT, incidentEventInput.whatValue) }
    incidentEventInput.howValue?.map { statusEventMap.put(SchemaEventTypeNames.HOW, incidentEventInput.howValue) }

    val incidentResults = fetchRelevantEventsByTargetType(
        statusEventMap = statusEventMap,
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


    // Response 3: Similar Tasks
    val potentialTasks = mutableListOf<EventDetails>()
    if (taskEventInput != null) {
        val taskStatusEventMap = mutableMapOf<SchemaEventTypeNames, String>()
        taskEventInput.whatValue?.map { statusEventMap.put(SchemaEventTypeNames.TASK, taskEventInput.whatValue) }
        taskEventInput.whyValue?.map { statusEventMap.put(SchemaEventTypeNames.WHY, taskEventInput.whyValue) }
        val taskResults = fetchRelevantEventsByTargetType(
            statusEventMap = taskStatusEventMap,
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

    // Response 4: Relevant personnel
//    val taskingMap = mutableMapOf<String, List<UserNodeEntity>>()
//
//    potentialTasks?.forEach { task ->
//        val taskHandlerMap = mutableListOf<Triple<UserNodeEntity, Int, Float>>()
//        val testDescNodeList = eventRepository.getNeighborsOfEventNodeById(task.eventId).filter { it.type == SchemaEventTypeNames.WHO.key }
//
//        if (testDescNodeList.isNotEmpty()) {
//            testDescNodeList.forEach { testNode ->
//                val nearbyPersonnelMap = findRelevantPersonnelByLocationUseCase(
//                    userActionRepository = userActionRepository,
//                    embeddingRepository = embeddingRepository,
//                    threatLocation = threatLocation,
//                    threatDescription = testNode.description,
//                    radiusInMeters = 5000f,
//                )
//                if (nearbyPersonnelMap != null) {
//                    taskHandlerMap.addAll(nearbyPersonnelMap.take(5))
//                }
//            }
//        } else {
//            val taskNode = eventRepository.getEventNodeByNameAndType(task.eventName, SchemaEventTypeNames.TASK.key)
//            if (taskNode != null) {
//                val method = eventRepository.getNeighborsOfEventNodeById(taskNode.id)
//                    .first { it.type == SchemaEventTypeNames.HOW.key }.name
//
//                val taskDescription = task.eventName + method
//                val nearbyPersonnelMap = findRelevantPersonnelByLocationUseCase(
//                    userActionRepository = userActionRepository,
//                    embeddingRepository = embeddingRepository,
//                    threatLocation = threatLocation,
//                    threatDescription = taskDescription,
//                    radiusInMeters = 5000f,
//                )
//                if (nearbyPersonnelMap != null) {
//                    taskHandlerMap.addAll(nearbyPersonnelMap.take(5))
//                }
//            }
//        }
//
//        if (taskHandlerMap.isNotEmpty()) {
//            val top3 = taskHandlerMap.sortedBy { it.second }.sortedByDescending { it.third }
//                .distinctBy { it.first.identifier }.take(3)
//                .associate { it.first to it.second}
//            taskingMap.put(task.eventName, top3.keys.toList())
//        }
//    }

    // Create function response
    val incidentResponse = ThreatAlertResponse(
        potentialImpacts = potentialImpacts,
        potentialTasks = potentialTasks,
        similarIncidents = similarIncidents
    )

    return incidentResponse
}