package com.example.graphapp.backend.usecases

import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.api.ThreatAlertResponse
import com.example.graphapp.backend.core.recommendEventsForProps
import com.example.graphapp.backend.dto.GraphSchema.PropertyNames
import com.example.graphapp.data.api.EventDetailData
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/*
Function to suspicious events in database
 */
suspend fun findRelatedSuspiciousEventsUseCase(
    eventInput: EventDetailData,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository
) : ThreatAlertResponse {

    val similarIncidentsFound = mutableMapOf<String, List<EventDetails>>()

    val incidentName = eventInput.whatValue
    val incidentMethod = eventInput.howValue
    if (incidentMethod != null || incidentName != null) {
        val (_, _, methodRecs) = recommendEventsForProps(
            newEventMap = buildMap<String, String> {
                incidentName?.let { put(PropertyNames.INCIDENT.key, it) }
                incidentMethod?.let { put(PropertyNames.HOW.key, it) }
            },
            eventRepository = eventRepository,
            embeddingRepository = embeddingRepository,
            queryKey = PropertyNames.INCIDENT.key
        )

        if (methodRecs.predictedEvents.isNotEmpty()) {
            val similarEventsByMethod = methodRecs.predictedEvents[PropertyNames.INCIDENT.key]
            if (similarEventsByMethod != null) {
                similarIncidentsFound.put(PropertyNames.HOW.key, similarEventsByMethod)
            }
        }
    }

    val incidentLocation = eventInput.whereValue
    if (incidentLocation != null) {
        val (_, _, locationRecs) = recommendEventsForProps(
            newEventMap = mapOf<String, String>(
                PropertyNames.WHERE.key to incidentLocation
            ),
            eventRepository = eventRepository,
            embeddingRepository = embeddingRepository,
            queryKey = PropertyNames.INCIDENT.key,
            getTopThreeResultsOnly = false
        )

        val locationResults = mutableListOf<EventDetails>()
        if (locationRecs.predictedEvents.isNotEmpty()) {
            val similarEventsByLocation = locationRecs.predictedEvents[PropertyNames.INCIDENT.key]
            if (similarEventsByLocation != null) {

                locationResults.addAll(similarEventsByLocation.sortedBy { it.eventProperties[PropertyNames.WHEN.key]!! })
                similarIncidentsFound.put(
                    PropertyNames.WHERE.key,
                    similarEventsByLocation.sortedBy { it.eventProperties[PropertyNames.WHEN.key]!! }
                )
            }
        }
    }

    val incidentResponse = ThreatAlertResponse(
        similarIncidents = if (similarIncidentsFound.isNotEmpty()) {
            similarIncidentsFound
        } else { null }
    )

    return incidentResponse
}