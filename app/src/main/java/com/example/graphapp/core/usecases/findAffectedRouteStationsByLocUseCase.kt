package com.example.graphapp.core.usecases

import com.example.graphapp.core.schema.GraphSchema.DisruptionCause
import com.example.graphapp.core.analyser.computeSimilarAndRelatedEvents
import com.example.graphapp.core.schema.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.core.schema.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.core.model.dto.EventDetails
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import kotlin.collections.isNotEmpty

/**
 * Implemented for Use Case 3: Route Integrity Check
 * 1. Searches database for any incidents near any of the route coordinates.
 * 2. Checks for airborne incidents.
 */
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
        val locationRecs = computeSimilarAndRelatedEvents(
            newEventMap = mapOf(SchemaEventTypeNames.WHERE to station),
            eventRepository = eventRepository,
            embeddingRepository = embeddingRepository,
            targetEventType = SchemaKeyEventTypeNames.INCIDENT,
            activeNodesOnly = true
        )

        if (locationRecs.isNotEmpty()) {
            val nearbyIncidents = locationRecs[SchemaKeyEventTypeNames.INCIDENT]
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