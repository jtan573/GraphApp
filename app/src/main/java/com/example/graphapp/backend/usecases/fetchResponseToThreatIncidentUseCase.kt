package com.example.graphapp.backend.usecases

import com.example.graphapp.backend.dto.GraphSchema.PropertyNames
import com.example.graphapp.data.api.EventDetailData
import com.example.graphapp.data.api.EventType
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
    )


    // Response 2: Similar Incidents -> What are the possible impacts?
    val statusEventMap = mutableMapOf<PropertyNames, String>()
    eventInput.whatValue?.map { statusEventMap.put(PropertyNames.INCIDENT, eventInput.whatValue) }
    eventInput.howValue?.map { statusEventMap.put(PropertyNames.HOW, eventInput.howValue) }

    val (_, _ ,impactResults) = fetchRelevantEventsByTargetType(
        statusEventMap = statusEventMap,
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        sourceEventType = EventType.INCIDENT,
        queryKey = EventType.IMPACT,
        activeNodesOnly = false
    )
    val potentialImpacts = if (impactResults.predictedEvents.isNotEmpty()) {
        impactResults.predictedEvents[EventType.IMPACT]?.map { it }
    } else { null }


    // Response 3: Similar incidents -> Tasks
    val (_, _ ,taskResults) = fetchRelevantEventsByTargetType(
        statusEventMap = statusEventMap,
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        sourceEventType = EventType.INCIDENT,
        queryKey = EventType.TASK,
        activeNodesOnly = false
    )
    val potentialTasks = if (taskResults.predictedEvents.isNotEmpty()) {
        taskResults.predictedEvents[EventType.TASK]?.map { it }
    } else { null }


    // Response 4: Relevant personnel
    val taskingMap = mutableMapOf<String, List<UserNodeEntity>>()
    potentialTasks?.forEach { task ->
        val taskNode = eventRepository.getEventNodeByNameAndType(task.eventName, PropertyNames.TASK.key)
        if (taskNode != null) {
            val method = eventRepository.getNeighborsOfEventNodeById(taskNode.id)
                .first { it.type == PropertyNames.HOW.key }.name

            val taskDescription = taskNode.name + method
            val nearbyPersonnelMap = findRelevantPersonnelByLocationUseCase(
                userActionRepository = userActionRepository,
                embeddingRepository = embeddingRepository,
                threatLocation = threatLocation,
                threatDescription = taskDescription,
                radiusInMeters = 5000f,
            )
            if (nearbyPersonnelMap != null) {
                val top3 = nearbyPersonnelMap.entries.take(3).associate { it.key to it.value }
                taskingMap.put("${task.eventName}: $method", top3.keys.toList())
            }
        }
    }

    // Create function response
    val incidentResponse = ThreatAlertResponse(
        nearbyActiveUsersMap = nearbyPersonnelMap,
        potentialImpacts = potentialImpacts,
        potentialTasks = potentialTasks,
        taskAssignment = taskingMap
    )

    return incidentResponse
}