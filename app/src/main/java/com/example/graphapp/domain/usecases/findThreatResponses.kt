package com.example.graphapp.domain.usecases

import android.util.Log
import com.example.graphapp.data.api.ProvideRecommendationsResponse
import com.example.graphapp.data.db.UserNodeEntity
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.repository.UserActionRepository
import com.example.graphapp.data.schema.QueryResult.IncidentResponse

// App response to INCIDENTS
suspend fun findThreatResponses(
    normalizedMap: Map<String, String>,
    userActionRepository: UserActionRepository,
    embeddingRepository: EmbeddingRepository,
    eventRepository: EventRepository,
    simMatrix: Map<Pair<Long, Long>, Float>,
): IncidentResponse {

    // Response 1: Active personnel with related specialisation
    val threatLocation = normalizedMap["Location"]
    val nearbyPersonnelMap = findRelevantPersonnelByLocationUseCase(
        userActionRepository = userActionRepository,
        embeddingRepository = embeddingRepository,
        threatLocation = threatLocation,
        radiusInMeters = 3000f
    )

    // Response 2: Similar Incidents -> What are the possible impacts?
    val relevantProperties = listOf<String>("Incident", "Method", "Motive")
    val (_, _ ,impactResults) = findRelevantEventsUseCase(
        statusEventMap = normalizedMap.filter { it.key in relevantProperties },
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        simMatrix = simMatrix,
        queryKey = "Impact"
    )
    val potentialImpacts = if (impactResults is ProvideRecommendationsResponse
        && impactResults.recommendations.isNotEmpty()) {
        impactResults.recommendations["Impact"]
    } else { null }

    // Response 3: Similar incidents -> Tasks
    val (_, _ ,taskResults) = findRelevantEventsUseCase(
        statusEventMap = normalizedMap,
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        simMatrix = simMatrix,
        queryKey = "Task"
    )
    val potentialTasks = if (taskResults is ProvideRecommendationsResponse
        && taskResults.recommendations.isNotEmpty()) {
        taskResults.recommendations["Task"]
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
    val incidentResponse = IncidentResponse(
        nearbyActiveUsersMap = nearbyPersonnelMap,
        potentialImpacts = potentialImpacts,
        potentialTasks = potentialTasks,
        taskAssignment = taskingMap
    )

    return incidentResponse
}