package com.example.graphapp.domain.usecases

import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.local.recommendEventsForProps
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.schema.QueryResult.IncidentResponse

suspend fun findAffectedRouteStationsByLocUseCase(
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    routeStations: List<String>? = null,
    threshold: Float = 0.8f
) : Map<Int, List<EventDetails>>? {

    if (routeStations == null || routeStations.isEmpty()) {
        return null
    }

    val nearbyIncidents = mutableMapOf<Int, List<EventDetails>>()
    routeStations.forEachIndexed { index, station ->
        val (_, _, locationRecs) = recommendEventsForProps(
            newEventMap = mapOf("Location" to station),
            eventRepository = eventRepository,
            embeddingRepository = embeddingRepository,
            queryKey = "Incident",
            getTopThreeResultsOnly = true,
            customThreshold = threshold
        )
        if (locationRecs.predictedEvents.isNotEmpty()) {
            val incidentsFound = locationRecs.predictedEvents["Incident"]
            if (incidentsFound != null) {
                nearbyIncidents.put(index, incidentsFound)
            }
        }
    }

    return nearbyIncidents
}