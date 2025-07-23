package com.example.graphapp.domain.viewmodellogic

import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.api.ProvideRecommendationsResponse
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.repository.UserActionRepository
import com.example.graphapp.data.schema.QueryResult.IncidentResponse
import com.example.graphapp.domain.usecases.findNearbyPersonnelByLocationUseCase
import com.example.graphapp.domain.usecases.findRelevantEventsUseCase
import com.example.graphapp.domain.usecases.findSimilarEventByLoc
import com.example.graphapp.domain.usecases.findSimilarEventByMethod

// App response to INCIDENTS
suspend fun createIncidentsResponse(
    normalizedMap: Map<String, String>,
    userActionRepository: UserActionRepository,
    embeddingRepository: EmbeddingRepository,
    eventRepository: EventRepository,
    simMatrix: Map<Pair<Long, Long>, Float>,
): IncidentResponse {

    // Response 1: Active personnel with related specialisation
    val threatLocation = normalizedMap["Location"]
    val threatDescription = normalizedMap["Description"]
    val nearbyPersonnelMap = findNearbyPersonnelByLocationUseCase(
        userActionRepository = userActionRepository,
        embeddingRepository = embeddingRepository,
        threatLocation = threatLocation,
        threatDescription = threatDescription,
        radiusInMeters = 3000f
    )

    // Response 2: Similar Incidents -> What are the possible impacts?
    val (_, _ ,impactResults) = findRelevantEventsUseCase(
        statusEventMap = normalizedMap,
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        simMatrix = simMatrix,
        queryKey = "Impact"
    )
    val potentialImpacts = if (impactResults is ProvideRecommendationsResponse
        && impactResults.recommendations.isNotEmpty()) {
        impactResults.recommendations["Impact"]
    } else { null }

    // Response 3: Similar Incidents by Method/Location -> Suspicious?
    val similarIncidentsFound = mutableMapOf<String, List<EventDetails>>()
    val similarIncidentsFoundByMethod = findSimilarEventByMethod(
        statusEventMap = normalizedMap,
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        queryKey = "Incident"
    )
    if (similarIncidentsFoundByMethod != null) {
        similarIncidentsFound.put("Method", similarIncidentsFoundByMethod)
    }
    val similarIncidentsFoundByLocation = findSimilarEventByLoc(
        statusEventMap = normalizedMap,
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        queryKey = "Incident"
    )
    if (similarIncidentsFoundByLocation != null) {
        similarIncidentsFound.put("Location", similarIncidentsFoundByLocation)
    }

    // Create function response
    val incidentResponse = IncidentResponse(
        nearbyActiveUsersMap = nearbyPersonnelMap,
        potentialImpacts = potentialImpacts,
        similarIncidents = if (similarIncidentsFound.isNotEmpty()) {
            similarIncidentsFound
        } else { null }
    )

    return incidentResponse
}