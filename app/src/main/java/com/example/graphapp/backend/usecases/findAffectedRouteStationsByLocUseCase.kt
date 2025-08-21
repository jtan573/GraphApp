package com.example.graphapp.backend.usecases

import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.backend.core.computeSimilarAndRelatedEvents
import com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.core.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.data.api.DisruptionCause
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository

suspend fun findAffectedRouteStationsByLocUseCase(
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    routeStations: List<String>? = null,
    threshold: Float = 0.5f
) : Map<DisruptionCause, List<EventDetails>>? {

    if (routeStations == null || routeStations.isEmpty()) {
        return null
    }

    val incidentsFoundMap = mutableMapOf<DisruptionCause, List<EventDetails>>()

    val proximityIncidentsFound = mutableListOf<EventDetails>()
    routeStations.forEachIndexed { index, station ->
        val (_, _, locationRecs) = computeSimilarAndRelatedEvents(
            newEventMap = mapOf(SchemaEventTypeNames.WHERE to station),
            eventRepository = eventRepository,
            embeddingRepository = embeddingRepository,
            targetEventType = SchemaKeyEventTypeNames.INCIDENT,
            getTopThreeResultsOnly = true,
            customThreshold = threshold,
            activeNodesOnly = true
        )

        if (locationRecs.predictedEvents.isNotEmpty()) {
            val nearbyIncidents = locationRecs.predictedEvents[SchemaKeyEventTypeNames.INCIDENT]
            nearbyIncidents?.forEach { incident ->
                val isAlreadyAdded = proximityIncidentsFound.any { it.eventId == incident.eventId }
                if (!isAlreadyAdded) {
                    proximityIncidentsFound.add(incident)
                }
            }
        }
    }
    if (proximityIncidentsFound.isNotEmpty()) {
        incidentsFoundMap.put(DisruptionCause.PROXIMITY, proximityIncidentsFound)
    }

    val windIncidentsFound = predictImpactOfWindAtLocationUseCase(
        routeStations, eventRepository, embeddingRepository
    )
    if (windIncidentsFound != null && windIncidentsFound.isNotEmpty()) {
        incidentsFoundMap.put(DisruptionCause.WIND, windIncidentsFound)
    }

    return incidentsFoundMap
}