package com.example.graphapp.backend.usecases

import android.util.Log
import com.example.graphapp.backend.dto.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.dto.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.data.api.EventDetailData
import com.example.graphapp.data.api.ThreatAlertResponse
import com.example.graphapp.data.db.UserNodeEntity
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.repository.UserActionRepository

// App response to INCIDENTS
suspend fun fetchResponseToThreatIncidentUseCase(
    eventInput: EventDetailData,
    userActionRepository: UserActionRepository,
    embeddingRepository: EmbeddingRepository,
    eventRepository: EventRepository,
): ThreatAlertResponse {

    // Response 1: Active personnel with related specialisation
    val threatLocation = eventInput.whereValue
    val nearbyPersonnelMap = findRelevantPersonnelByLocationUseCase(
        userActionRepository = userActionRepository,
        embeddingRepository = embeddingRepository,
        threatLocation = threatLocation,
        radiusInMeters = 3000f
    )?.associate { it.first to it.second }


    // Response 2: Similar Incidents -> What are the possible impacts?
    val statusEventMap = mutableMapOf<SchemaEventTypeNames, String>()
    eventInput.whatValue?.map { statusEventMap.put(SchemaEventTypeNames.INCIDENT, eventInput.whatValue) }
    eventInput.howValue?.map { statusEventMap.put(SchemaEventTypeNames.HOW, eventInput.howValue) }

    // test
    val (_, _ , incidentResults) = fetchRelevantEventsByTargetType(
        statusEventMap = statusEventMap,
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        sourceEventType = SchemaKeyEventTypeNames.INCIDENT,
        queryKey = SchemaKeyEventTypeNames.INCIDENT,
        activeNodesOnly = false
    )
    val similarIncidents = if (incidentResults.predictedEvents.isNotEmpty()) {
        incidentResults.predictedEvents[SchemaKeyEventTypeNames.INCIDENT]?.map { it }
    } else { null }

    val potentialImpacts = mutableMapOf<Long, List<String>>()
    if (!similarIncidents.isNullOrEmpty()) {
        similarIncidents.forEach { simIncident ->
            val impacts = eventRepository.getNeighborsOfEventNodeById(simIncident.eventId)
                .filter { it.type == SchemaKeyEventTypeNames.IMPACT.key }.map { it.name }
            potentialImpacts.put(simIncident.eventId, impacts)
        }
    } else { null }

//    val (_, _ ,impactResults) = fetchRelevantEventsByTargetType(
//        statusEventMap = statusEventMap,
//        eventRepository = eventRepository,
//        embeddingRepository = embeddingRepository,
//        sourceEventType = SchemaKeyEventTypeNames.INCIDENT,
//        queryKey = SchemaKeyEventTypeNames.IMPACT,
//        activeNodesOnly = false
//    )
//    val potentialImpacts = if (impactResults.predictedEvents.isNotEmpty()) {
//        impactResults.predictedEvents[SchemaKeyEventTypeNames.IMPACT]?.map { it }
//    } else { null }


//    // Response 3: Similar incidents -> Tasks
//    val (_, _ ,taskResults) = fetchRelevantEventsByTargetType(
//        statusEventMap = statusEventMap,
//        eventRepository = eventRepository,
//        embeddingRepository = embeddingRepository,
//        sourceEventType = SchemaKeyEventTypeNames.INCIDENT,
//        queryKey = SchemaKeyEventTypeNames.TASK,
//        activeNodesOnly = false
//    )
//    val potentialTasks = if (taskResults.predictedEvents.isNotEmpty()) {
//        taskResults.predictedEvents[SchemaKeyEventTypeNames.TASK]?.map { it }
//    } else { null }


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
        nearbyActiveUsersMap = nearbyPersonnelMap,
        potentialImpacts = potentialImpacts,
//        potentialTasks = potentialTasks,
//        taskAssignment = taskingMap
        similarIncidents = similarIncidents
    )

    return incidentResponse
}