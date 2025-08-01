package com.example.graphapp.backend.usecases

import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.backend.core.recommendEventsForProps
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository

suspend fun findAffectedRouteStationsByLocUseCase(
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    routeStations: List<String>? = null,
    threshold: Float = 0.5f
) : Map<String, List<EventDetails>>? {

    if (routeStations == null || routeStations.isEmpty()) {
        return null
    }

    val incidentsFoundMap = mutableMapOf<String, List<EventDetails>>()

    val proximityIncidentsFound = mutableListOf<EventDetails>()
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
            val nearbyIncidents = locationRecs.predictedEvents["Incident"]
            nearbyIncidents?.forEach { incident ->
                val isAlreadyAdded = proximityIncidentsFound.any { it.eventId == incident.eventId }
                if (!isAlreadyAdded) {
                    proximityIncidentsFound.add(incident)
                }
            }
        }
    }
    if (proximityIncidentsFound.isNotEmpty()) {
        incidentsFoundMap.put("Proximity", proximityIncidentsFound)
    }

    val windIncidentsFound = predictImpactOfWindAtLocationUseCase(
        routeStations, eventRepository, embeddingRepository
    )
    if (windIncidentsFound != null && windIncidentsFound.isNotEmpty()) {
        incidentsFoundMap.put("Wind", windIncidentsFound)
    }

    return incidentsFoundMap
}