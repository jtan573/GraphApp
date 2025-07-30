package com.example.graphapp.domain.usecases

import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.local.recommendEventsForProps
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.schema.QueryResult.IncidentResponse
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/*
Function to suspicious events in database
 */
suspend fun findRelatedSuspiciousEventsUseCase(
    normalizedMap: Map<String, String>,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository
) : IncidentResponse {

    val similarIncidentsFound = mutableMapOf<String, List<EventDetails>>()

    val incidentName = normalizedMap["Incident"]
    val incidentMethod = normalizedMap["Method"]
    if (incidentMethod != null || incidentName != null) {
        val (_, _, methodRecs) = recommendEventsForProps(
            newEventMap = buildMap<String, String> {
                incidentName?.let { put("Incident", it) }
                incidentMethod?.let { put("Method", it) }
            },
            eventRepository = eventRepository,
            embeddingRepository = embeddingRepository,
            queryKey = "Incident"
        )

        if (methodRecs.predictedEvents.isNotEmpty()) {
            val similarEventsByMethod = methodRecs.predictedEvents["Incident"]
            if (similarEventsByMethod != null) {
                similarIncidentsFound.put("Method", similarEventsByMethod)
            }
        }
    }

    val incidentLocation = normalizedMap["Location"]
    if (incidentLocation != null) {
        val (_, _, locationRecs) = recommendEventsForProps(
            newEventMap = mapOf<String, String>(
                "Location" to incidentLocation
            ),
            eventRepository = eventRepository,
            embeddingRepository = embeddingRepository,
            queryKey = "Incident",
            getTopThreeResultsOnly = false
        )

        val locationResults = mutableListOf<EventDetails>()
        if (locationRecs.predictedEvents.isNotEmpty()) {
            val similarEventsByLocation = locationRecs.predictedEvents["Incident"]
            if (similarEventsByLocation != null) {
                val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mmX", Locale.US)
                formatter.timeZone = TimeZone.getTimeZone("UTC+8")

                locationResults.addAll(similarEventsByLocation.sortedBy { formatter.parse(it.eventProperties["Date"]!!) })
                similarIncidentsFound.put(
                    "Location",
                    similarEventsByLocation.sortedBy { formatter.parse(it.eventProperties["Date"]!!) }
                )
            }
        }
    }

    val incidentResponse = IncidentResponse(
        similarIncidents = if (similarIncidentsFound.isNotEmpty()) {
            similarIncidentsFound
        } else { null }
    )

    return incidentResponse
}