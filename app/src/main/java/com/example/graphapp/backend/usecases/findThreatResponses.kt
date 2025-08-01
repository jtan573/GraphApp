package com.example.graphapp.backend.usecases

import com.example.graphapp.data.api.DiscoverEventsResponse
import com.example.graphapp.data.api.EventDetailData
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.api.ThreatAlertResponse
import com.example.graphapp.data.db.UserNodeEntity
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.repository.UserActionRepository

// App response to INCIDENTS
suspend fun findThreatResponses(
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
    val statusEventMap = mutableMapOf<String, String>()
    eventInput.whatValue?.map { statusEventMap.put("Incident", eventInput.whatValue) }
    eventInput.howValue?.map { statusEventMap.put("Method", eventInput.howValue) }

    val (_, _ ,impactResults) = findRelevantEventsUseCase(
        statusEventMap = statusEventMap,
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        queryKey = "Impact"
    )
    val potentialImpacts = if (impactResults is DiscoverEventsResponse
        && impactResults.predictedEvents.isNotEmpty()) {
        impactResults.predictedEvents["Impact"]?.map { it.eventName }
    } else { null }


    // Response 3: Similar incidents -> Tasks
    val (_, _ ,taskResults) = findRelevantEventsUseCase(
        statusEventMap = statusEventMap,
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        queryKey = "Task"
    )
    val potentialTasks = if (taskResults is DiscoverEventsResponse
        && taskResults.predictedEvents.isNotEmpty()) {
        taskResults.predictedEvents["Task"]?.map { it.eventName }
    } else { null }


    // Response 4: Relevant personnel
    val taskingMap = mutableMapOf<String, List<UserNodeEntity>>()
    potentialTasks?.forEach { task ->
        val taskNode = eventRepository.getEventNodeByNameAndType(task, "Task")
        if (taskNode != null) {
            val method = eventRepository.getNeighborsOfEventNodeById(taskNode.id)
                .first { it.type == "Method" }.name

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
                taskingMap.put("$task: $method", top3.keys.toList())
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